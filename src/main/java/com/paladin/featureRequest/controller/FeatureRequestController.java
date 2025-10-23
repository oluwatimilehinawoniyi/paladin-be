package com.paladin.featureRequest.controller;

import com.paladin.auth.services.JwtService;
import com.paladin.common.dto.FeatureRequestCreateDTO;
import com.paladin.common.dto.FeatureRequestDTO;
import com.paladin.common.dto.FeatureRequestUpdateDTO;
import com.paladin.common.enums.FeatureRequestStatus;
import com.paladin.common.response.ResponseHandler;
import com.paladin.featureRequest.service.FeatureRequestService;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feature-requests")
@RequiredArgsConstructor
@Slf4j
public class FeatureRequestController {

    private final FeatureRequestService featureRequestService;
    private final JwtService jwtService;
    private final UserRepository userRepository;


    // Create a new feature request
    @PostMapping
    public ResponseEntity<FeatureRequestDTO> createFeatureRequest(
            @Valid @RequestBody FeatureRequestCreateDTO dto,
            HttpServletRequest request) {

        User user = getUserFromRequest(request);
        log.info("Creating feature request for user: {}", user.getEmail());

        FeatureRequestDTO created = featureRequestService.createFeatureRequest(dto, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Get all feature requests (with optional status filter)
    @GetMapping
    public ResponseEntity<Object> getAllFeatureRequests(
            @RequestParam(required = false) FeatureRequestStatus status,
            HttpServletRequest request) {

        User user = null;
        try {
            user = getUserFromRequest(request);
        } catch (Exception e) {
            log.debug("No authenticated user for feature requests list");
        }

        UUID userId = user != null ? user.getId() : null;
        List<FeatureRequestDTO> requests = featureRequestService.getAllFeatureRequests(userId, status);
        return ResponseHandler.responseBuilder(
                "Feature requests retrieved successfully",
                HttpStatus.OK,
                requests
        );
    }

    // Get single feature request by ID
    @GetMapping("/{id}")
    public ResponseEntity<FeatureRequestDTO> getFeatureRequest(
            @PathVariable UUID id,
            HttpServletRequest request) {

        User user = null;
        try {
            user = getUserFromRequest(request);
        } catch (Exception e) {
            log.debug("No authenticated user for feature request detail");
        }

        UUID userId = user != null ? user.getId() : null;
        FeatureRequestDTO featureRequest = featureRequestService.getFeatureRequestById(id, userId);
        return ResponseEntity.ok(featureRequest);
    }

    // Get current user's own feature requests
    @GetMapping("/my-requests")
    public ResponseEntity<Object> getMyFeatureRequests(HttpServletRequest request) {
        User user = getUserFromRequest(request);
        List<FeatureRequestDTO> requests = featureRequestService.getUserFeatureRequests(user.getId());

        return ResponseHandler.responseBuilder(
                "User requests retrieved successfully",
                HttpStatus.OK,
                requests
        );
    }

    // Update own feature request (only if PENDING)
    @PutMapping("/{id}")
    public ResponseEntity<FeatureRequestDTO> updateFeatureRequest(
            @PathVariable UUID id,
            @Valid @RequestBody FeatureRequestUpdateDTO dto,
            HttpServletRequest request) {

        User user = getUserFromRequest(request);
        FeatureRequestDTO updated = featureRequestService.updateFeatureRequest(id, dto, user.getId());
        return ResponseEntity.ok(updated);
    }

    // Delete own feature request (only if PENDING)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeatureRequest(
            @PathVariable UUID id,
            HttpServletRequest request) {

        User user = getUserFromRequest(request);
        featureRequestService.deleteFeatureRequest(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    // Upvote a feature request
    @PostMapping("/{id}/upvote")
    public ResponseEntity<Map<String, Object>> upvoteFeatureRequest(
            @PathVariable UUID id,
            HttpServletRequest request) {

        User user = getUserFromRequest(request);
        Long totalVotes = featureRequestService.upvoteFeatureRequest(id, user.getId());
        return ResponseEntity.ok(Map.of(
                "message", "Upvoted successfully",
                "totalVotes", totalVotes
        ));
    }

    // Remove upvote from a feature request
    @DeleteMapping("/{id}/upvote")
    public ResponseEntity<Map<String, Object>> removeUpvote(
            @PathVariable UUID id,
            HttpServletRequest request) {

        User user = getUserFromRequest(request);
        Long totalVotes = featureRequestService.removeUpvote(id, user.getId());
        return ResponseEntity.ok(Map.of(
                "message", "Vote removed",
                "totalVotes", totalVotes
        ));
    }

    // Check if current user has voted for a feature request
    @GetMapping("/{id}/my-vote")
    public ResponseEntity<Map<String, Boolean>> hasUserVoted(
            @PathVariable UUID id,
            HttpServletRequest request) {

        User user = getUserFromRequest(request);
        boolean hasVoted = featureRequestService.hasUserVoted(id, user.getId());
        return ResponseEntity.ok(Map.of("hasVoted", hasVoted));
    }

    // Helper method to get user from JWT token
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
