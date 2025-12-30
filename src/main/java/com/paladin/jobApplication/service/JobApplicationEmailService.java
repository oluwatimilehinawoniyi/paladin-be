package com.paladin.jobApplication.service;

import java.io.IOException;
import java.util.UUID;

public interface JobApplicationEmailService {
    public void sendJobApplicationEmail(
            UUID userId,
            String toEmail,
            String subject,
            String bodyText,
            UUID cvId
    ) throws IOException, jakarta.mail.MessagingException;
}
