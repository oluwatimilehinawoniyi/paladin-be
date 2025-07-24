package com.paladin.user.service;

import com.paladin.enums.AuthProvider;
import com.paladin.user.User;
import com.paladin.user.interfaces.OAuth2UserInfo;
import com.paladin.user.model.OAuth2UserInfoFactory;
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
import java.util.Optional;

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
        OAuth2UserInfo userInfo =
                OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId,
                        oauth2User.getAttributes());

        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException(
                    "Email not found from OAuth2 provider");
        }

        Optional<User> userOptional =
                userRepository.findByEmail(userInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            // User exists, update if necessary
            user = userOptional.get();
            if (!user.getAuthProvider().name()
                    .equalsIgnoreCase(registrationId)) {
                throw new OAuth2AuthenticationException(
                        "User is already registered with " + user.getAuthProvider() + " provider"
                );
            }
            updateExistingUser(user, userInfo);
        } else {
            // Create a new user
            registerNewUser(userRequest, userInfo);
        }

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("USER")),
                oauth2User.getAttributes(),
                userInfo.getNameAttributeKey()
        );
    }

    private void registerNewUser(OAuth2UserRequest userRequest,
                                 OAuth2UserInfo userInfo) {
        User user = new User();
        user.setEmail(userInfo.getEmail());
        user.setFirstName(userInfo.getFirstName());
        user.setLastName(userInfo.getLastName());
        user.setPassword(""); // OAuth2 users don't have passwords
        user.setAuthProvider(
                AuthProvider.valueOf(userRequest.getClientRegistration()
                        .getRegistrationId().toUpperCase()));
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    private void updateExistingUser(User existingUser,
                                    OAuth2UserInfo userInfo) {
        existingUser.setFirstName(userInfo.getFirstName());
        existingUser.setLastName(userInfo.getLastName());

        userRepository.save(existingUser);
    }
}
