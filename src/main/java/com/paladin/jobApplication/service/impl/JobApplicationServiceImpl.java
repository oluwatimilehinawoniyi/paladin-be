package com.paladin.jobApplication.service.impl;

import com.paladin.dto.JobApplicationDTO;
import com.paladin.dto.NewJobApplicationDTO;
import com.paladin.enums.ApplicationStatus;
import com.paladin.exceptions.CVNotFoundException;
import com.paladin.exceptions.CannotSendMailException;
import com.paladin.exceptions.ProfileNotFoundException;
import com.paladin.exceptions.UnauthorizedAccessException;
import com.paladin.jobApplication.JobApplication;
import com.paladin.jobApplication.repository.JobApplicationRepository;
import com.paladin.jobApplication.service.JobApplicationService;
import com.paladin.mappers.JobApplicationMapper;
import com.paladin.profile.Profile;
import com.paladin.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final JobApplicationMapper jobApplicationMapper;
    private final ProfileRepository profileRepository; // To link with profile
    private final JobApplicationEmailServiceImpl jobApplicationEmailService; // To send email

    /**
     * Creates and sends a job application.
     *
     * @param dto    The DTO containing application details.
     * @param userId The ID of the user.
     * @return The created JobApplicationDTO.
     */
    public JobApplicationDTO createAndSendJobApplication(
            NewJobApplicationDTO dto,
            UUID userId) {
        Profile profile = profileRepository.findById(dto.profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

        if (!profile.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Unauthorized: Profile does not belong to user");
        }

        if (profile.getCv() == null) {
            throw new CVNotFoundException("Profile does not have a CV linked to it.");
        }

        JobApplication jobApplication = jobApplicationMapper.toEntity(dto);
        jobApplication.setProfile(profile);
        jobApplication.setSentAt(LocalDateTime.now());
        jobApplication.setStatus(ApplicationStatus.SENT); // Initial status

        try {
            jobApplicationEmailService.sendJobApplicationEmail(
                    userId,
                    dto.getJobEmail(),
                    dto.subject,
                    dto.bodyText,
                    profile.getCv().getId()
            );
        } catch (Exception e) {
            // Handle email sending failure, maybe log and throw a custom exception
            throw new CannotSendMailException("Failed to send job application email: " + e.getMessage(), e);
        }

        return jobApplicationMapper.toDTO(jobApplicationRepository.save(jobApplication));
    }

    /**
     * Lists job applications by a user id.
     *
     * @param userId The ID of the user.
     * @return List of job applications of a user.
     */
    public List<JobApplicationDTO> getJobApplicationsByUserId(UUID userId) {
        return jobApplicationRepository.findByProfileUserId(userId)
                .stream()
                .map(jobApplicationMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates the status of an application.
     *
     * @param applicationId The ID of the application.
     * @param newStatus The updated status of the application.
     * @param userId The ID of the user.
     * @return The updated job application.
     */
    public JobApplicationDTO updateJobApplicationStatus(UUID applicationId, ApplicationStatus newStatus, UUID userId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Job application not found"));

        if (!application.getProfile().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Application does not belong to user");
        }

        application.setStatus(newStatus);
        return jobApplicationMapper.toDTO(jobApplicationRepository.save(application));
    }
}