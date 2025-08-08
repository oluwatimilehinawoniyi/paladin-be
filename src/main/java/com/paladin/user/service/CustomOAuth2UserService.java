package com.paladin.user.service;

import com.paladin.enums.AuthProvider;
import com.paladin.user.User;
import com.paladin.user.model.GoogleOAuth2UserInfo;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception e) {
            log.error("Error processing OAuth2 user", e);
            throw new OAuth2AuthenticationException(
                    "Error processing OAuth2 user: " + e.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest,
                                         OAuth2User oauth2User) {
        String registrationId =
                userRequest.getClientRegistration().getRegistrationId();

        if (!"google".equalsIgnoreCase(registrationId)) {
            throw new OAuth2AuthenticationException("Only Google authentication is supported");
        }

        GoogleOAuth2UserInfo userInfo =
                new GoogleOAuth2UserInfo(oauth2User.getAttributes());

        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException(
                    "Email not found from Google");
        }

        User user =
                userRepository.findByEmail(userInfo.getEmail())
                        .orElse(null);

        if (user != null) {
            updateExistingUser(user, userInfo);
        } else {
            user = registerNewUser(userInfo);
        }

        user.setAccessToken(userRequest.getAccessToken().getTokenValue());
        user.setAccessTokenExpiry(
                userRequest.getAccessToken().getExpiresAt() != null ?
                        LocalDateTime.ofInstant(
                                userRequest.getAccessToken()
                                        .getExpiresAt(),
                                java.time.ZoneOffset.UTC) : null);

        userRepository.save(user);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("USER")),
                oauth2User.getAttributes(),
                userInfo.getNameAttributeKey()
        );
    }

    private User registerNewUser(GoogleOAuth2UserInfo userInfo) {
        User user = new User();
        user.setEmail(userInfo.getEmail());
        user.setFirstName(userInfo.getFirstName());
        user.setLastName(
                userInfo.getLastName() != null ? userInfo.getLastName() :
                        "");
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setCreatedAt(LocalDateTime.now());

        log.info("Registering new Google user: {}", userInfo.getEmail());
        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser,
                                    GoogleOAuth2UserInfo userInfo) {
        existingUser.setFirstName(userInfo.getFirstName());
        existingUser.setLastName(
                userInfo.getLastName() != null ? userInfo.getLastName() :
                        "");

        log.info("Updating existing Google user: {}", userInfo.getEmail());
        return userRepository.save(existingUser);
    }
}
