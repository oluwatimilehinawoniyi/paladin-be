package com.paladin.jobApplication.service.impl;

import com.paladin.auth.interfaces.EmailProvider;
import com.paladin.dto.JobApplicationEmailRequest;
import com.paladin.enums.AuthProvider;
import com.paladin.user.User;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EmailProviderService {
    private final GmailEmailProvider gmailProvider;

    public void sendJobApplicationEmail(User user, JobApplicationEmailRequest request)
            throws IOException, MessagingException {

        EmailProvider provider = getEmailProvider(user);
        provider.sendJobApplicationEmail(user, request);
    }

    public boolean canUserSendEmails(User user) {
        EmailProvider provider = getEmailProvider(user);
        return provider.canSendEmails(user);
    }

    private EmailProvider getEmailProvider(User user) {
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            return gmailProvider;
        }
        throw new RuntimeException("Email provider not supported for user's authentication method: " +
                user.getAuthProvider());
    }
}
