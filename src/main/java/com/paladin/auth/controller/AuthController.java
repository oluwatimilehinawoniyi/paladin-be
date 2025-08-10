package com.paladin.auth.controller;

import com.paladin.enums.AuthProvider;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully",
                "httpStatus", "OK"
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", oauth2User.getAttribute("email"));
        userInfo.put("firstName", oauth2User.getAttribute("given_name"));
        userInfo.put("lastName", oauth2User.getAttribute("family_name"));

        return ResponseEntity.ok(Map.of(
                "data", Map.of("user", userInfo),
                "message", "User authenticated",
                "httpStatus", "OK"
        ));
    }

    @GetMapping("/callback")
    public void handleOAuthCallback(HttpServletResponse response) throws IOException, IOException {
        // Redirect to frontend callback page
        response.sendRedirect("http://localhost:5173/auth/callback");
    }

    @RestController
    @RequestMapping("/api/debug")
    @RequiredArgsConstructor
    @Slf4j
    public static class DebugController {

        private final UserRepository userRepository;

        @GetMapping("/users")
        public Map<String, Object> getAllUsers() {
            try {
                List<User> users = userRepository.findAll();
                log.info("Found {} users in database", users.size());

                return Map.of(
                        "success", true,
                        "userCount", users.size(),
                        "users", users
                );
            } catch (Exception e) {
                log.error("Error fetching users: ", e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage()
                );
            }
        }

        @PostMapping("/create-test-user")
        public Map<String, Object> createTestUser(@RequestParam String email) {
            try {
                User testUser = new User();
                testUser.setEmail(email);
                testUser.setFirstName("Test");
                testUser.setLastName("User");
                testUser.setAuthProvider(AuthProvider.GOOGLE);
                testUser.setCreatedAt(LocalDateTime.now());

                User savedUser = userRepository.save(testUser);
                log.info("Created test user: {}", savedUser);

                return Map.of(
                        "success", true,
                        "user", savedUser
                );
            } catch (Exception e) {
                log.error("Error creating test user: ", e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage()
                );
            }
        }

        @GetMapping("/find-user/{email}")
        public Map<String, Object> findUserByEmail(@PathVariable String email) {
            try {
                User user = userRepository.findByEmail(email).orElse(null);

                return Map.of(
                        "success", true,
                        "found", user != null,
                        "user", user
                );
            } catch (Exception e) {
                log.error("Error finding user: ", e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage()
                );
            }
        }

        @DeleteMapping("/delete-user/{email}")
        public Map<String, Object> deleteUserByEmail(@PathVariable String email) {
            try {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    userRepository.delete(user);
                    return Map.of("success", true, "message", "User deleted");
                } else {
                    return Map.of("success", false, "message", "User not found");
                }
            } catch (Exception e) {
                log.error("Error deleting user: ", e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage()
                );
            }
        }

        @GetMapping("/oauth-test")
        public Map<String, Object> testOAuthFlow(Principal principal) {
            log.error("🧪🧪🧪 OAuth Test Endpoint Called 🧪🧪🧪");

            if (principal == null) {
                log.error("❌ Principal is null");
                return Map.of("error", "No principal", "authenticated", false);
            }

            log.error("✅ Principal type: {}", principal.getClass().getName());
            log.error("✅ Principal name: {}", principal.getName());

            if (principal instanceof OAuth2AuthenticationToken oauth2Token) {
                OAuth2User oauth2User = oauth2Token.getPrincipal();
                log.error("✅ OAuth2User attributes: {}", oauth2User.getAttributes());

                String email = oauth2User.getAttribute("email");
                log.error("✅ Email from OAuth2User: {}", email);

                // Check if user exists in database
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    log.error("✅ User found in database: {}", user.getId());
                } else {
                    log.error("❌ User NOT found in database for email: {}", email);
                }

                return Map.of(
                        "authenticated", true,
                        "principalType", "OAuth2AuthenticationToken",
                        "email", email != null ? email : "null",
                        "attributes", oauth2User.getAttributes(),
                        "userInDatabase", user != null,
                        "userId", user != null ? user.getId().toString() : "null"
                );
            }

            return Map.of(
                    "authenticated", true,
                    "principalType", principal.getClass().getName(),
                    "principalName", principal.getName(),
                    "userInDatabase", false
            );
        }
    }
}
