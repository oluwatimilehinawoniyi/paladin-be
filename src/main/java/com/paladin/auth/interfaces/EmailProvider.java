package com.paladin.auth.interfaces;

import com.paladin.dto.JobApplicationEmailRequest;
import com.paladin.enums.AuthProvider;
import com.paladin.user.User;
import jakarta.mail.MessagingException;

import java.io.IOException;

public interface EmailProvider {
    void sendJobApplicationEmail(
            User user,
            JobApplicationEmailRequest request
    ) throws IOException, MessagingException;

    boolean canSendEmails(User user);
    AuthProvider getSupportedProvider();
}
