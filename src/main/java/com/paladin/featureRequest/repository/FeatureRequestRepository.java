package com.paladin.featureRequest.repository;

import org.springframework.stereotype.Repository;

import com.paladin.common.enums.FeatureRequestStatus;
import com.paladin.featureRequest.FeatureRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeatureRequestRepository extends JpaRepository<FeatureRequest, UUID> {

    // Find all requests by a specific user
    List<FeatureRequest> findByUserId(UUID userId);

    // Find all requests with a specific status
    List<FeatureRequest> findByStatus(FeatureRequestStatus status);

    // Get all requests with vote counts, ordered by votes DESC
    @Query("SELECT fr, COUNT(v) as voteCount " +
            "FROM FeatureRequest fr " +
            "LEFT JOIN FeatureRequestVote v ON v.featureRequest.id = fr.id " +
            "GROUP BY fr " +
            "ORDER BY voteCount DESC, fr.createdAt DESC")
    List<Object[]> findAllWithVoteCount();

    // Get requests by status with vote counts
    @Query("SELECT fr, COUNT(v) as voteCount " +
            "FROM FeatureRequest fr " +
            "LEFT JOIN FeatureRequestVote v ON v.featureRequest.id = fr.id " +
            "WHERE fr.status = :status " +
            "GROUP BY fr " +
            "ORDER BY voteCount DESC, fr.createdAt DESC")
    List<Object[]> findByStatusWithVoteCount(@Param("status") FeatureRequestStatus status);

    // Count votes for a specific feature request
    @Query("SELECT COUNT(v) FROM FeatureRequestVote v WHERE v.featureRequest.id = :featureRequestId")
    Long countVotesByFeatureRequestId(@Param("featureRequestId") UUID featureRequestId);
}