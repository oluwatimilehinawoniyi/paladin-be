package com.paladin.jobApplication.service.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.model.Message;
import com.paladin.auth.interfaces.EmailProvider;
import com.paladin.common.enums.ApplicationStatus;
import com.paladin.config.GoogleOAuthConfig;
import com.paladin.common.dto.JobApplicationEmailRequest;
import com.paladin.common.enums.AuthProvider;
import com.paladin.jobApplication.repository.JobApplicationRepository;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.google.api.services.gmail.Gmail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class GmailEmailProvider implements EmailProvider {
    private final GoogleOAuthConfig googleOAuthConfig;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;

    private static final String APPLICATION_NAME = "Paladin Job Application";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();


    @Override
    @Async
    @CircuitBreaker(name = "gmailService", fallbackMethod = "fallbackSendEmail")
    @Retryable(
            retryFor = {IOException.class, MessagingException.class},
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 2000,
                    multiplier = 2.0,
                    maxDelay = 10000
            )
    )
    public void sendJobApplicationEmail(
            User user,
            JobApplicationEmailRequest request
    ) throws IOException, MessagingException {
        log.info("Attempting to send email to {} (async with retry + circuit breaker)", request.getToEmail());

        Credential credential = createCredential(user);
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("Paladin Job Application")
                .build();

        MimeMessage emailContent = createEmailWithAttachment(
                user.getEmail(),
                request.getToEmail(),
                request.getSubject(),
                request.getBodyText(),
                request.getCvData(),
                request.getCvContentType(),
                request.getCvFileName()
        );

        Message message = createMessageWithEmail(emailContent);
        service.users().messages().send(user.getEmail(), message).execute();
        log.info("Email sent successfully via Gmail API to {}", request.getToEmail());
    }

    /**
     * Circuit breaker fallback - called when Gmail circuit is OPEN.
     * Marks job application as FAILED_TO_SEND so user can see it in their dashboard.
     */
    public void fallbackSendEmail(
            User user,
            JobApplicationEmailRequest request,
            Exception e
    ) {
        log.error("Circuit breaker OPEN for Gmail service. Fast-failing email to {}",
                request.getToEmail());
        log.error("Reason: {}", e.getMessage());

        // PRODUCTION READY: Mark job application as failed
        markJobApplicationAsFailed(user.getId(), request.getToEmail(),
                "Gmail service temporarily unavailable (circuit breaker open)");

        // TODO for future:
        // 1. Queue email in DLQ for automatic retry when circuit closes
        // 2. Send in-app notification to user
    }

    /**
     * Retry recovery fallback - called when all retry attempts fail.
     * Marks job application as FAILED_TO_SEND so user can retry manually.
     */
    @Recover
    public void recoverFromEmailFailure(
            IOException e,
            User user,
            JobApplicationEmailRequest request
    ) {
        log.error("✗ Failed to send email to {} after all retries. Error: {}",
                request.getToEmail(),
                e.getMessage());
        log.error("User: {}, Job: {}, Error details: ",
                user.getEmail(),
                request.getSubject(),
                e);

        // PRODUCTION READY: Mark job application as failed
        markJobApplicationAsFailed(user.getId(), request.getToEmail(),
                "Failed after " + 3 + " retry attempts: " + e.getMessage());

        // TODO for future:
        // 1. Store failed email in persistent DLQ (database table or Redis)
        // 2. Send email to user about delivery failure
    }

    /**
     * Helper method to mark job application as FAILED_TO_SEND.
     * Allows user to see failed applications in dashboard and retry manually.
     */
    private void markJobApplicationAsFailed(UUID userId, String jobEmail, String reason) {
        try {
            jobApplicationRepository.findTopByProfileUserIdAndJobEmailOrderBySentAtDesc(
                    userId,
                    jobEmail
            ).ifPresentOrElse(
                    jobApp -> {
                        jobApp.setStatus(ApplicationStatus.FAILED_TO_SEND);
                        jobApp.setSentAt(LocalDateTime.now());
                        jobApplicationRepository.save(jobApp);
                        log.info("Marked job application {} as FAILED_TO_SEND. User can retry from dashboard.",
                                jobApp.getId());
                    },
                    () -> log.warn("Could not find job application for user {} and email {} to mark as failed",
                            userId, jobEmail)
            );
        } catch (Exception ex) {
            // Don't fail the whole request if we can't update status
            log.error("Failed to update job application status: {}", ex.getMessage());
        }
    }

    @Override
    public boolean canSendEmails(User user) {
        return user.getAuthProvider() == AuthProvider.GOOGLE &&
                user.getAccessToken() != null &&
                user.getRefreshToken() != null;
    }

    @Override
    public AuthProvider getSupportedProvider() {
        return AuthProvider.GOOGLE;
    }


    private GoogleClientSecrets createClientSecrets() {
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(googleOAuthConfig.getClientId());
        details.setClientSecret(googleOAuthConfig.getClientSecret());
        clientSecrets.setInstalled(details);
        return clientSecrets;
    }

    private Credential createCredential(User user) throws IOException {
        GoogleAuthorizationCodeFlow flow = createAuthFlow();
        Credential credential = createCredentialFromFlow(flow);

        credential.setAccessToken(user.getAccessToken());
        credential.setRefreshToken(user.getRefreshToken());
        if (user.getAccessTokenExpiry() != null) {
            credential.setExpirationTimeMilliseconds(
                    user.getAccessTokenExpiry().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
            );
        }

        if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() < 300) {
            log.info("Access token near expiry, attempting to refresh.");
            boolean refreshed = credential.refreshToken();
            if (refreshed) {
                updateUserTokens(user, credential);
            }
        }
        return credential;
    }

    private void refreshAccessToken(User user) throws IOException {
        GoogleAuthorizationCodeFlow flow = createAuthFlow();
        Credential credential = createCredentialFromFlow(flow);
        credential.setRefreshToken(user.getRefreshToken());

        try {
            boolean refreshed = credential.refreshToken();
            if (refreshed) {
                updateUserTokens(user, credential);
                log.info("Access token refreshed successfully for user {}", user.getEmail());
            } else {
                log.warn("Failed to refresh access token for user {}. Refresh token might be invalid.", user.getEmail());
                throw new RuntimeException("Failed to refresh access token. User might need to re-authenticate.");
            }
        } catch (IOException e) {
            log.error("Error during token refresh for user {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Error refreshing access token", e);
        }
    }

    private GoogleAuthorizationCodeFlow createAuthFlow() {
        GoogleClientSecrets clientSecrets = createClientSecrets();
        return new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                clientSecrets,
                Collections.singletonList("https://www.googleapis.com/auth/gmail.send")
        )
                .setAccessType("offline")
                .build();
    }

    private Credential createCredentialFromFlow(GoogleAuthorizationCodeFlow flow) {
        return new Credential.Builder(flow.getMethod())
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setTokenServerUrl(new GenericUrl(flow.getTokenServerEncodedUrl()))
                .setClientAuthentication(flow.getClientAuthentication())
                .build();
    }

    private void updateUserTokens(User user, Credential credential) {
        user.setAccessToken(credential.getAccessToken());
        user.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(credential.getExpiresInSeconds()));
        if (credential.getRefreshToken() != null) {
            user.setRefreshToken(credential.getRefreshToken());
        }
        userRepository.save(user);
    }

    private MimeMessage createEmailWithAttachment(
            String from,
            String to,
            String subject,
            String bodyText,
            byte[] attachmentData,
            String attachmentContentType,
            String attachmentFileName
    ) throws jakarta.mail.MessagingException, IOException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);

        MimeMultipart multipart = new MimeMultipart();

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(bodyText);
        multipart.addBodyPart(textPart);

        if (attachmentData != null && attachmentData.length > 0) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setContent(attachmentData, attachmentContentType);
            attachmentPart.setFileName(attachmentFileName);
            multipart.addBodyPart(attachmentPart);
        }

        email.setContent(multipart);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent)
            throws jakarta.mail.MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
        return new Message().setRaw(encodedEmail);
    }
}
