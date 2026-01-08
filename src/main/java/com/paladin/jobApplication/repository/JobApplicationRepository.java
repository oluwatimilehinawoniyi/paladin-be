package com.paladin.jobApplication.repository;

import com.paladin.jobApplication.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByProfileUserId(UUID userId);

    // Find most recent job application by user and job email
    Optional<JobApplication> findTopByProfileUserIdAndJobEmailOrderBySentAtDesc(
            UUID userId,
            String jobEmail
    );
}
