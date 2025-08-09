package com.paladin.auth.controller;

import com.paladin.dto.UserDTO;
import com.paladin.exceptions.UserNotFoundException;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
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
}
