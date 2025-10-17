package com.paladin.notification.controller;

import com.paladin.auth.services.JwtService;
import com.paladin.common.dto.NotificationDTO;
import com.paladin.common.response.ResponseHandler;
import com.paladin.notification.service.NotificationService;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Notifications", description = "User notification management endpoints")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Get user's notifications with pagination
     */
    @GetMapping
    @Operation(summary = "Get user's notifications",
            description = "Returns paginated list of user's notifications, newest first")
    public ResponseEntity<Object> getUserNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        User user = getUserFromRequest(request);
        Page<NotificationDTO> notifications = notificationService
                .getUserNotifications(user.getId(), page, size);

        return ResponseHandler.responseBuilder(
                "Notifications retrieved successfully",
                HttpStatus.OK,
                notifications
        );
    }

    /**
     * Get unread notifications only
     */
    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications",
            description = "Returns all unread notifications for the current user")
    public ResponseEntity<Object> getUnreadNotifications(HttpServletRequest request) {

        User user = getUserFromRequest(request);
        List<NotificationDTO> notifications = notificationService
                .getUnreadNotifications(user.getId());

        return ResponseHandler.responseBuilder(
                "Unread notifications retrieved successfully",
                HttpStatus.OK,
                notifications
        );
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count",
            description = "Returns the count of unread notifications (for badge)")
    public ResponseEntity<Object> getUnreadCount(HttpServletRequest request) {

        User user = getUserFromRequest(request);
        Long count = notificationService.getUnreadCount(user.getId());

        return ResponseHandler.responseBuilder(
                "Unread count retrieved successfully",
                HttpStatus.OK,
                Map.of("unreadCount", count)
        );
    }

    /**
     * Mark specific notification as read
     */
    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read",
            description = "Marks a specific notification as read")
    public ResponseEntity<Object> markAsRead(
            @PathVariable UUID id,
            HttpServletRequest request) {

        User user = getUserFromRequest(request);
        notificationService.markAsRead(id, user.getId());

        return ResponseHandler.responseBuilder(
                "Notification marked as read",
                HttpStatus.OK,
                null
        );
    }

    /**
     * Mark all notifications as read
     */
    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read",
            description = "Marks all user's notifications as read")
    public ResponseEntity<Object> markAllAsRead(HttpServletRequest request) {

        User user = getUserFromRequest(request);
        notificationService.markAllAsRead(user.getId());

        return ResponseHandler.responseBuilder(
                "All notifications marked as read",
                HttpStatus.OK,
                null
        );
    }

    /**
     * Helper method to extract user from JWT token
     */
    private User getUserFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("No valid authorization token");
        }

        String token = authHeader.substring(7);
        String userEmail = jwtService.extractUsername(token);

        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}


