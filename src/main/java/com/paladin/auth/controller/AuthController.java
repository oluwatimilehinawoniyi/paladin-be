package com.paladin.auth.controller;

import com.paladin.auth.services.JwtService;
import com.paladin.common.dto.TokenRefreshRequestDTO;
import com.paladin.common.dto.TokenRefreshResponseDTO;
import com.paladin.common.exceptions.NotFoundException;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Get current authenticated user information
     */
    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId().toString());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(Map.of(
                "data", Map.of("user", userInfo),
                "message", "User authenticated",
                "httpStatus", "OK"
        ));
    }

    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequestDTO request) {
        try {
            String refreshToken = request.getRefreshToken();

            if (!jwtService.isRefreshTokenValid(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "message", "Invalid or expired refresh token",
                                "httpStatus", "UNAUTHORIZED"
                        ));
            }

            // extract user information from refresh token
            String email = jwtService.extractUsername(refreshToken);
            String userId = jwtService.extractUserId(refreshToken);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(email)
                    .password("")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            String newAccessToken = jwtService.generateAccessToken(userDetails, userId);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails, userId);

            TokenRefreshResponseDTO response = TokenRefreshResponseDTO.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpirationInSeconds())
                    .build();

            log.info("Token refreshed successfully for user: {}", email);

            return ResponseEntity.ok(Map.of(
                    "data", response,
                    "message", "Token refreshed successfully",
                    "httpStatus", "OK"
            ));

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Failed to refresh token: " + e.getMessage(),
                            "httpStatus", "UNAUTHORIZED"
                    ));
        }
    }

    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        log.info("Logout requested");

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully. Please clear tokens on client side.",
                "httpStatus", "OK"
        ));
    }
}
