package com.paladin.auth;

import com.paladin.enums.AuthProvider;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

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
                User user = userRepository.findByEmail(email).orElseGet(() -> {
                    log.error("Creating new user: {}", email);

                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName(firstName != null ? firstName : "");
                    newUser.setLastName(lastName != null ? lastName : "");
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    newUser.setCreatedAt(LocalDateTime.now());

                    User savedUser = userRepository.save(newUser);
                    log.error("Successfully created user: {}", savedUser.getId());
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
                        log.info("Refresh token updated for user: {}", email);
                    }

                    userRepository.save(user);
                    log.error("Updated user tokens: {}", user.getId());
                }
            }
        }

        log.info("OAuth2 authentication completed, redirecting to frontend");

        String targetUrl = "http://localhost:5173/auth/callback";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}