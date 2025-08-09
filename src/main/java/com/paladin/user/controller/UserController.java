package com.paladin.user.controller;

import com.paladin.dto.UserDTO;
import com.paladin.mappers.UserMapper;
import com.paladin.response.ResponseHandler;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import com.paladin.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Object> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = principal.getName();
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        // Map the User entity to UserResponseDTO
        UserDTO userResponseDTO = userMapper.toDTO(user);
        return ResponseHandler.responseBuilder(
                "User details successfully returned",
                HttpStatus.OK,
                userResponseDTO
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Object> deleteCurrentUser(
            Principal principal,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            String userEmail = principal.getName();
            log.info("User {} requested account deletion", userEmail);

            // Delete user and all associated data (domino effect)
            userService.deleteUserByEmail(userEmail);

            // Clear security context and invalidate session
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                log.info("Session invalidated for deleted user");
            }

            log.info("User {} successfully deleted", userEmail);

            return ResponseHandler.responseBuilder(
                    "Account successfully deleted",
                    HttpStatus.OK,
                    Map.of("message", "Your account and all associated data have been permanently deleted")
            );

        } catch (Exception e) {
            log.error("Failed to delete user account: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete account: " + e.getMessage()));
        }
    }
}