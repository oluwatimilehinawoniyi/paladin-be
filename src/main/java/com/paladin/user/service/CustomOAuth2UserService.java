package com.paladin.user.service;

import com.paladin.enums.AuthProvider;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        log.error("OAuth2User attributes: {}", oAuth2User.getAttributes());
        log.error("OAuth2User name: {}", oAuth2User.getName());

        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        log.error("Extracted - Email: {}, FirstName: {}, LastName: {}", email, firstName, lastName);

        if (email == null) {
            log.error("EMAIL IS NULL! Cannot create user without email.");
            throw new OAuth2AuthenticationException("Email not provided by OAuth2 provider");
        }

        try {
            User user = findOrCreateUser(email, firstName, lastName);
            log.error("User processing completed successfully: {}", user.getEmail());

            User verifyUser = userRepository.findByEmail(email).orElse(null);
            if (verifyUser != null) {
                log.info("VERIFICATION DONE: User exists in database with ID: {}", verifyUser.getId());
            } else {
                log.error("VERIFICATION FAILED: User was not saved to database!");
            }

        } catch (Exception e) {
            log.error("ERROR during user creation/lookup: ", e);
            throw new OAuth2AuthenticationException("Failed to process user");
        }
        return oAuth2User;
    }

    private User findOrCreateUser(String email, String firstName, String lastName) {
        log.error("Looking for existing user with email: {}", email);

        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    log.error("Found existing user: {} with ID: {}", email, existingUser.getId());
                    return existingUser;
                })
                .orElseGet(() -> {
                    log.error("User not found, creating new user: {}", email);

                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName(firstName != null ? firstName : "");
                    newUser.setLastName(lastName != null ? lastName : "");
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    newUser.setCreatedAt(LocalDateTime.now());

                    try {
                        User savedUser = userRepository.save(newUser);
                        log.info("Successfully saved new user with ID: {} and email: {}",
                                savedUser.getId(), savedUser.getEmail());
                        return savedUser;
                    } catch (Exception e) {
                        log.error("FAILED to save user to database: ", e);
                        throw new RuntimeException("Failed to save user", e);
                    }
                });
    }
}