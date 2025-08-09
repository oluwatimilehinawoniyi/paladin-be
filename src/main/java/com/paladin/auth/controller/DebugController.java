package com.paladin.auth.controller;

import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import com.paladin.enums.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

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
}