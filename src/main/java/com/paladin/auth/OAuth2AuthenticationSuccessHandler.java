package com.paladin.auth;

import com.paladin.auth.services.JwtService;
import com.paladin.common.enums.AuthProvider;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * OAuth2 Success Handler - Updated for JWT
 * Generates JWT tokens after successful Google OAuth and redirects to frontend with tokens
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final JwtService jwtService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        log.info("OAuth2 authentication successful");

        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();

            String email = oauth2User.getAttribute("email");
            String firstName = oauth2User.getAttribute("given_name");
            String lastName = oauth2User.getAttribute("family_name");

            if (email != null) {
                // Find or create user in database
                User user = userRepository.findByEmail(email).orElseGet(() -> {
                    log.info("Creating new user: {}", email);

                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName(firstName != null ? firstName : "");
                    newUser.setLastName(lastName != null ? lastName : "");
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    newUser.setCreatedAt(LocalDateTime.now());

                    User savedUser = userRepository.save(newUser);
                    log.info("Successfully created user: {}", savedUser.getId());
                    return savedUser;
                });

                OAuth2AuthorizedClient authorizedClient =
                        authorizedClientService.loadAuthorizedClient(
                                oauth2Token.getAuthorizedClientRegistrationId(),
                                oauth2Token.getName()
                        );

                if (authorizedClient != null) {
                    user.setAccessToken(authorizedClient.getAccessToken().getTokenValue());
                    user.setAccessTokenExpiry(
                            authorizedClient.getAccessToken().getExpiresAt() != null ?
                                    LocalDateTime.ofInstant(
                                            authorizedClient.getAccessToken().getExpiresAt(),
                                            java.time.ZoneOffset.UTC) :
                                    null);

                    if (authorizedClient.getRefreshToken() != null) {
                        user.setRefreshToken(authorizedClient.getRefreshToken().getTokenValue());
                        log.info("Google OAuth refresh token updated for user: {}", email);
                    }

                    userRepository.save(user);
                    log.info("Updated user Google OAuth tokens: {}", user.getId());
                }

                UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                        .username(email)
                        .password("")
                        .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                        .build();

                String accessToken = jwtService.generateAccessToken(userDetails, user.getId().toString());
                String refreshToken = jwtService.generateRefreshToken(userDetails, user.getId().toString());

                log.info("Generated JWT tokens for user: {}", email);

//                String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
//                        .queryParam("accessToken", accessToken)
//                        .queryParam("refreshToken", refreshToken)
//                        .build()
//                        .toUriString();

                String targetUrl = frontendUrl + "/auth/callback#accessToken=" + accessToken + "&refreshToken=" + refreshToken;

                log.info("OAuth2 authentication completed, redirecting to frontend with JWT tokens");
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
            } else {
                log.error("Email not found in OAuth2 response");
                String errorUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/login")
                        .queryParam("error", "oauth_failed")
                        .build()
                        .toUriString();
                getRedirectStrategy().sendRedirect(request, response, errorUrl);
            }
        }
    }
}