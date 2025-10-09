package com.paladin.featureRequest.service;

import com.paladin.common.dto.*;
import com.paladin.common.enums.FeatureRequestStatus;
import com.paladin.common.exceptions.FeatureRequestNotFoundException;
import com.paladin.common.exceptions.UnauthorizedAccessException;
import com.paladin.featureRequest.FeatureRequest;
import com.paladin.featureRequest.FeatureRequestVote;
import com.paladin.featureRequest.repository.FeatureRequestRepository;
import com.paladin.featureRequest.repository.FeatureRequestVoteRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureRequestService {

    private final FeatureRequestRepository featureRequestRepository;
    private final FeatureRequestVoteRepository voteRepository;
    private final UserRepository userRepository;

    @Transactional
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

    @Transactional
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

    @Transactional
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

    @Transactional
    public Long upvoteFeatureRequest(UUID featureRequestId, UUID userId) {
        FeatureRequest request = featureRequestRepository.findById(featureRequestId)
                .orElseThrow(() -> new FeatureRequestNotFoundException("Feature request not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (voteRepository.existsByFeatureRequestIdAndUserId(featureRequestId, userId)) {
            throw new IllegalStateException("Already voted for this feature request");
        }

        FeatureRequestVote vote = FeatureRequestVote.builder()
                .featureRequest(request)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        voteRepository.save(vote);

        return featureRequestRepository.countVotesByFeatureRequestId(featureRequestId);
    }

    @Transactional
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

    @Transactional
    public FeatureRequestDTO updateStatus(UUID id, FeatureRequestStatusUpdateDTO dto) {
        FeatureRequest request = featureRequestRepository.findById(id)
                .orElseThrow(() -> new FeatureRequestNotFoundException("Feature request not found"));

        request.setStatus(dto.getStatus());
        request.setAdminResponse(dto.getAdminResponse());
        request.setUpdatedAt(LocalDateTime.now());

        FeatureRequest updated = featureRequestRepository.save(request);
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
}