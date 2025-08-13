package com.paladin.auth.services;

import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Scheduled(fixedRate = 3000000)
    public void refreshExpiredTokens() {
        log.info("Starting automatic token refresh check...");

        LocalDateTime expiryThreshold = LocalDateTime.now().plusMinutes(15);

        List<User> usersNeedingRefresh = userRepository.findUsersWithExpiringTokens(expiryThreshold);

        if (usersNeedingRefresh.isEmpty()) {
            log.info("No tokens need refreshing");
            return;
        }

        log.info("Found {} users needing token refresh", usersNeedingRefresh.size());

        for (User user : usersNeedingRefresh) {
            try {
                refreshUserToken(user);
            } catch (Exception e) {
                log.error("Failed to refresh token for user {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }

    private void refreshUserToken(User user) {
        if (user.getRefreshToken() == null) {
            log.warn("User {} has no refresh token, skipping", user.getEmail());
            return;
        }

        try {
            log.info("Refreshing token for user: {}", user.getEmail());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", user.getRefreshToken());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/token",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();

                user.setAccessToken((String) tokenResponse.get("access_token"));

                Integer expiresIn = (Integer) tokenResponse.get("expires_in");
                if (expiresIn != null) {
                    user.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
                }

                String newRefreshToken = (String) tokenResponse.get("refresh_token");
                if (newRefreshToken != null) {
                    user.setRefreshToken(newRefreshToken);
                }

                userRepository.save(user);
                log.info("Successfully refreshed token for user: {}", user.getEmail());

            } else {
                log.error("Failed to refresh token for user {}: {}", user.getEmail(), response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Exception refreshing token for user {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
