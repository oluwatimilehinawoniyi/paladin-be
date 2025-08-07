package com.paladin.jobApplication.service.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.paladin.config.GoogleOAuthConfig;
import com.paladin.cv.CV;
import com.paladin.cv.service.impl.CVServiceImpl;
import com.paladin.exceptions.UserNotFoundException;
import com.paladin.jobApplication.service.JobApplicationEmailService;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
public class JobApplicationEmailServiceImpl implements JobApplicationEmailService {

    private final CVServiceImpl cvServiceImpl;
    private final UserRepository userRepository;
    private final GoogleOAuthConfig googleConfig;

    private static final String APPLICATION_NAME = "Paladin Job Application";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    public void sendJobApplicationEmail(
            UUID userId,
            String toEmail,
            String subject,
            String bodyText,
            UUID cvId
    ) throws IOException, jakarta.mail.MessagingException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getAccessToken() == null || user.getRefreshToken() == null) {
            throw new RuntimeException("User not authenticated with Google or tokens are missing.");
        }

        Credential credential = createCredential(user);
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        CV cv = cvServiceImpl.getCVByIdAsEntity(cvId);

        byte[] cvData = null;
        if (cv != null) {
            cvData = cvServiceImpl.downloadCV(cv.getId(), userId);
        }

        MimeMessage emailContent = createEmailWithAttachment(
                user.getEmail(),
                toEmail,
                subject,
                bodyText,
                cvData,
                cv != null ? cv.getContentType() : null,
                cv != null ? cv.getFileName() : null
        );

        Message message = createMessageWithEmail(emailContent);

        try {
            service.users().messages().send(user.getEmail(), message).execute();
            log.info("Email sent successfully to {}", toEmail);
        } catch (IOException e) {
            log.error("Failed to send email: {}", e.getMessage());
            if (e.getMessage().contains("Token has been expired or revoked")) {
                log.warn("Access token expired, attempting to refresh...");
                refreshAccessToken(user);
                throw new RuntimeException("Failed to send email due to expired token. Please try again or re-authenticate.", e);
            }
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private GoogleClientSecrets createClientSecrets() {
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(googleConfig.getClientId());
        details.setClientSecret(googleConfig.getClientSecret());
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