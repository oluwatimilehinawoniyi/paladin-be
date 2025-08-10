package com.paladin.auth;

import com.paladin.enums.AuthProvider;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
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
            throws IOException, ServletException {

        log.error("🎉🎉🎉 SUCCESS HANDLER IS BEING CALLED! 🎉🎉🎉");

        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();

            log.error("🎉 OAuth2User attributes: {}", oauth2User.getAttributes());

            String email = oauth2User.getAttribute("email");
            String firstName = oauth2User.getAttribute("given_name");
            String lastName = oauth2User.getAttribute("family_name");

            log.error("🎉 Extracted - Email: {}, FirstName: {}, LastName: {}", email, firstName, lastName);

            if (email != null) {
                // Find or create user - THIS IS WHERE WE'LL CREATE THE USER!
                User user = userRepository.findByEmail(email).orElseGet(() -> {
                    log.error("🆕 Creating new user in success handler: {}", email);

                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName(firstName != null ? firstName : "");
                    newUser.setLastName(lastName != null ? lastName : "");
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    newUser.setCreatedAt(LocalDateTime.now());

                    User savedUser = userRepository.save(newUser);
                    log.error("✅ Successfully created user in success handler: {}", savedUser.getId());
                    return savedUser;
                });

                // Get the authorized client which contains the refresh token
                OAuth2AuthorizedClient authorizedClient =
                        authorizedClientService.loadAuthorizedClient(
                                oauth2Token.getAuthorizedClientRegistrationId(),
                                oauth2Token.getName()
                        );

                if (authorizedClient != null) {
                    // Update tokens from the authorized client
                    user.setAccessToken(authorizedClient.getAccessToken().getTokenValue());
                    user.setAccessTokenExpiry(
                            authorizedClient.getAccessToken().getExpiresAt() != null ?
                                    LocalDateTime.ofInstant(
                                            authorizedClient.getAccessToken().getExpiresAt(),
                                            java.time.ZoneOffset.UTC) :
                                    null);

                    // Set refresh token if available
                    if (authorizedClient.getRefreshToken() != null) {
                        user.setRefreshToken(authorizedClient.getRefreshToken().getTokenValue());
                        log.info("Refresh token updated for user: {}", email);
                    }

                    userRepository.save(user);
                    log.error("✅ Updated user tokens: {}", user.getId());
                }
            }
        }

        log.error("🎉🎉🎉 SUCCESS HANDLER COMPLETED - REDIRECTING TO FRONTEND 🎉🎉🎉");

        String targetUrl = "http://localhost:5173/auth/callback";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}