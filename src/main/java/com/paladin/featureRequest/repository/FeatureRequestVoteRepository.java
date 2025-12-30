package com.paladin.featureRequest.repository;

import com.paladin.featureRequest.FeatureRequestVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureRequestVoteRepository extends JpaRepository<FeatureRequestVote, UUID> {

    // Find a vote by feature request and user
    Optional<FeatureRequestVote> findByFeatureRequestIdAndUserId(
            UUID featureRequestId,
            UUID userId
    );

    // Check if a user has already voted for a feature request
    boolean existsByFeatureRequestIdAndUserId(
            UUID featureRequestId,
            UUID userId
    );

    // Delete a vote by feature request and user
    void deleteByFeatureRequestIdAndUserId(
            UUID featureRequestId,
            UUID userId
    );

    List<FeatureRequestVote> findByFeatureRequestId(UUID featureRequestId);
}