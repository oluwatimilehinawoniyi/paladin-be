package com.paladin.featureRequest.service;

import com.paladin.common.dto.*;
import com.paladin.common.enums.FeatureRequestStatus;
import com.paladin.common.enums.NotificationType;
import com.paladin.common.exceptions.FeatureRequestNotFoundException;
import com.paladin.common.exceptions.NotFoundException;
import com.paladin.common.exceptions.UnauthorizedAccessException;
import com.paladin.featureRequest.FeatureRequest;
import com.paladin.featureRequest.FeatureRequestVote;
import com.paladin.featureRequest.repository.FeatureRequestRepository;
import com.paladin.featureRequest.repository.FeatureRequestVoteRepository;
import com.paladin.notification.service.NotificationService;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.paladin.common.EntityTypes.FEATURE_REQUEST;
import static com.paladin.common.EntityTypes.SYSTEM;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FeatureRequestService {

    private final FeatureRequestRepository featureRequestRepository;
    private final FeatureRequestVoteRepository voteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FeatureRequestDTO createFeatureRequest(FeatureRequestCreateDTO dto, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FeatureRequest request = FeatureRequest.builder()
                .user(user)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .status(FeatureRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        FeatureRequest saved = featureRequestRepository.save(request);

        notificationService.notifyUser(
                userId,
                NotificationType.STATUS_UPDATE,
                "Feature request submitted!",
                "We've received your request: '" + dto.getTitle() + "'. We'll review it soon!",
                saved.getId(),
                FEATURE_REQUEST
        );

        return toDTO(saved, 0L, false);
    }

    @Transactional(readOnly = true)
    public List<FeatureRequestDTO> getAllFeatureRequests(UUID currentUserId, FeatureRequestStatus statusFilter) {
        List<Object[]> results;

        if (statusFilter != null) {
            results = featureRequestRepository.findByStatusWithVoteCount(statusFilter);
        } else {
            results = featureRequestRepository.findAllWithVoteCount();
        }

        List<FeatureRequestDTO> dto = new ArrayList<>();
        for (Object[] result : results) {
            FeatureRequest request = (FeatureRequest) result[0];
            Long voteCount = (Long) result[1];
            boolean hasVoted = currentUserId != null &&
                    voteRepository.existsByFeatureRequestIdAndUserId(request.getId(), currentUserId);
            dto.add(toDTO(request, voteCount, hasVoted));
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public FeatureRequestDTO getFeatureRequestById(UUID id, UUID currentUserId) {
        FeatureRequest request = featureRequestRepository.findById(id)
                .orElseThrow(() -> new FeatureRequestNotFoundException("Feature request not found"));

        Long voteCount = featureRequestRepository.countVotesByFeatureRequestId(id);
        boolean hasVoted = currentUserId != null &&
                voteRepository.existsByFeatureRequestIdAndUserId(id, currentUserId);

        return toDTO(request, voteCount, hasVoted);
    }

    @Transactional(readOnly = true)
    public List<FeatureRequestDTO> getUserFeatureRequests(UUID userId) {
        List<FeatureRequest> requests = featureRequestRepository.findByUserId(userId);

        return requests.stream()
                .map(request -> {
                    Long voteCount = featureRequestRepository.countVotesByFeatureRequestId(request.getId());
                    return toDTO(request, voteCount, false);
                })
                .collect(Collectors.toList());
    }

    public FeatureRequestDTO updateFeatureRequest(UUID id, FeatureRequestUpdateDTO dto, UUID userId) {
        FeatureRequest request = featureRequestRepository.findById(id)
                .orElseThrow(() -> new FeatureRequestNotFoundException("Feature request not found"));

        if (!request.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only update your own requests");
        }

        if (request.getStatus() != FeatureRequestStatus.PENDING) {
            throw new IllegalStateException("Can only update requests with PENDING status");
        }

        request.setTitle(dto.getTitle());
        request.setDescription(dto.getDescription());
        request.setCategory(dto.getCategory());
        request.setUpdatedAt(LocalDateTime.now());

        FeatureRequest updated = featureRequestRepository.save(request);
        Long voteCount = featureRequestRepository.countVotesByFeatureRequestId(id);

        return toDTO(updated, voteCount, false);
    }

    public void deleteFeatureRequest(UUID id, UUID userId) {
        FeatureRequest request = featureRequestRepository.findById(id)
                .orElseThrow(() -> new FeatureRequestNotFoundException("Feature request not found"));

        if (!request.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only delete your own requests");
        }

        if (request.getStatus() != FeatureRequestStatus.PENDING) {
            throw new IllegalStateException("Can only delete requests with PENDING status");
        }

        featureRequestRepository.delete(request);
    }

    public Long upvoteFeatureRequest(UUID featureRequestId, UUID userId) {
        FeatureRequest request = featureRequestRepository.findById(featureRequestId)
                .orElseThrow(() -> new FeatureRequestNotFoundException("Feature request not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (voteRepository.existsByFeatureRequestIdAndUserId(featureRequestId, userId)) {
            throw new IllegalStateException("Already voted for this feature request");
        }

        FeatureRequestVote vote = FeatureRequestVote.builder()
                .featureRequest(request)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        voteRepository.save(vote);

        UUID creatorId = request.getUser().getId();
        if (!creatorId.equals(userId)) {
            notificationService.notifyUser(
                    creatorId,
                    NotificationType.STATUS_UPDATE,
                    "Someone upvoted your request!",
                    user.getFirstName() + " supports '" + request.getTitle() + "'",
                    request.getId(),
                    FEATURE_REQUEST
            );
        }

        return featureRequestRepository.countVotesByFeatureRequestId(featureRequestId);
    }

    public Long removeUpvote(UUID featureRequestId, UUID userId) {
        if (!voteRepository.existsByFeatureRequestIdAndUserId(featureRequestId, userId)) {
            throw new IllegalStateException("You haven't voted for this feature request");
        }

        voteRepository.deleteByFeatureRequestIdAndUserId(featureRequestId, userId);

        return featureRequestRepository.countVotesByFeatureRequestId(featureRequestId);
    }

    @Transactional(readOnly = true)
    public boolean hasUserVoted(UUID featureRequestId, UUID userId) {
        return voteRepository.existsByFeatureRequestIdAndUserId(featureRequestId, userId);
    }

    public FeatureRequestDTO updateStatus(UUID id, FeatureRequestStatusUpdateDTO dto) {
        FeatureRequest request = featureRequestRepository.findById(id)
                .orElseThrow(() -> new FeatureRequestNotFoundException("Feature request not found"));

        FeatureRequestStatus oldStatus = request.getStatus();
        FeatureRequestStatus newStatus = dto.getStatus();

        boolean adminResponseChanged =
                (request.getAdminResponse() == null && dto.getAdminResponse() != null) ||
                        (request.getAdminResponse() != null && dto.getAdminResponse() != null &&
                                !request.getAdminResponse().equals(dto.getAdminResponse()));


        request.setStatus(newStatus);
        request.setAdminResponse(dto.getAdminResponse());
        request.setUpdatedAt(LocalDateTime.now());

        FeatureRequest updated = featureRequestRepository.save(request);

        triggerStatusChangeNotifications(request, oldStatus, newStatus);

        if (adminResponseChanged) {
            triggerAdminResponseNotification(request);
        }

        Long voteCount = featureRequestRepository.countVotesByFeatureRequestId(id);

        log.info("Feature request {} status updated to {}", id, dto.getStatus());

        return toDTO(updated, voteCount, false);
    }

    @Transactional(readOnly = true)
    public FeatureRequestStatsDTO getStats() {
        List<FeatureRequest> allRequests = featureRequestRepository.findAll();

        return FeatureRequestStatsDTO.builder()
                .total((long) allRequests.size())
                .pending(allRequests.stream().filter(r -> r.getStatus() == FeatureRequestStatus.PENDING).count())
                .underReview(allRequests.stream().filter(r -> r.getStatus() == FeatureRequestStatus.UNDER_REVIEW).count())
                .inProgress(allRequests.stream().filter(r -> r.getStatus() == FeatureRequestStatus.IN_PROGRESS).count())
                .completed(allRequests.stream().filter(r -> r.getStatus() == FeatureRequestStatus.COMPLETED).count())
                .rejected(allRequests.stream().filter(r -> r.getStatus() == FeatureRequestStatus.REJECTED).count())
                .build();
    }

    private FeatureRequestDTO toDTO(FeatureRequest request, Long voteCount, boolean hasVoted) {
        return FeatureRequestDTO.builder()
                .id(request.getId())
                .userId(request.getUser().getId())
                .userEmail(request.getUser().getEmail())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .categoryDisplayName(request.getCategory().getDisplayName())
                .status(request.getStatus())
                .statusDisplayName(request.getStatus().getDisplayName())
                .adminResponse(request.getAdminResponse())
                .voteCount(voteCount)
                .hasVoted(hasVoted)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private void triggerStatusChangeNotifications(
            FeatureRequest request,
            FeatureRequestStatus oldStatus,
            FeatureRequestStatus newStatus) {

        UUID creatorId = request.getUser().getId();
        String title = request.getTitle();

        switch (newStatus) {
            case UNDER_REVIEW:
                notificationService.notifyUser(
                        creatorId,
                        NotificationType.STATUS_UPDATE,
                        "Your request is under review",
                        "We're reviewing '" + title + "'",
                        request.getId(),
                        FEATURE_REQUEST
                );
                break;

            case IN_PROGRESS:
                notificationService.notifyUser(
                        creatorId,
                        NotificationType.STATUS_UPDATE,
                        "Work has started!",
                        "We're building '" + title + "'",
                        request.getId(),
                        FEATURE_REQUEST
                );

                notificationService.notifySubscribers(
                        request.getId(),
                        "Feature in progress",
                        "A feature you voted for (" + title + ") is being built!"
                );
                break;

            case COMPLETED:
                notificationService.notifyUser(
                        creatorId,
                        NotificationType.STATUS_UPDATE,
                        "Your feature is live!",
                        "'" + title + "' is now available!",
                        request.getId(),
                        FEATURE_REQUEST
                );

                notificationService.notifySubscribers(
                        request.getId(),
                        "Feature completed!",
                        "A feature you voted for (" + title + ") is now live!"
                );

                notificationService.notifyAllUsers(
                        "New Feature Available!",
                        "Check out: " + title
                );
                break;

            case REJECTED:
                notificationService.notifyUser(
                        creatorId,
                        NotificationType.STATUS_UPDATE,
                        "Update on your request",
                        "We've reviewed '" + title + "'. " + request.getAdminResponse(),
                        request.getId(),
                        FEATURE_REQUEST
                );
                break;
        }
    }

    private void triggerAdminResponseNotification(FeatureRequest request) {
        notificationService.notifyUser(
                request.getUser().getId(),
                NotificationType.ADMIN_RESPONSE,
                "Admin responded to your request",
                "Check the response on '" + request.getTitle() + "'",
                request.getId(),
                FEATURE_REQUEST
        );

        log.info("Admin response notification sent for feature request {}", request.getId());
    }

}