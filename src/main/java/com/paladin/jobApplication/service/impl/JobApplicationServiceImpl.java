package com.paladin.jobApplication.service.impl;

import com.paladin.common.dto.JobApplicationDTO;
import com.paladin.common.dto.JobApplicationEmailRequest;
import com.paladin.common.dto.NewJobApplicationDTO;
import com.paladin.cv.service.impl.CVServiceImpl;
import com.paladin.common.enums.ApplicationStatus;
import com.paladin.common.exceptions.CVNotFoundException;
import com.paladin.common.exceptions.CannotSendMailException;
import com.paladin.common.exceptions.ProfileNotFoundException;
import com.paladin.common.exceptions.UnauthorizedAccessException;
import com.paladin.jobApplication.JobApplication;
import com.paladin.jobApplication.repository.JobApplicationRepository;
import com.paladin.jobApplication.service.JobApplicationService;
import com.paladin.common.mappers.JobApplicationMapper;
import com.paladin.profile.Profile;
import com.paladin.profile.repository.ProfileRepository;
import com.paladin.user.User;
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
    private final ProfileRepository profileRepository;
    private final EmailProviderService emailProviderService; // Changed from direct Gmail service
    private final CVServiceImpl cvService;

    /**
     * Creates and sends a job application.
     *
     * @param application The application details to be used.
     * @param userId      The ID of the user.
     * @return The created JobApplicationDTO.
     */
    public JobApplicationDTO createAndSendJobApplication(
            NewJobApplicationDTO application,
            UUID userId) {
        Profile profile = profileRepository.findById(application.profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

        if (!profile.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Unauthorized: Profile does not belong to user");
        }

        if (profile.getCv() == null) {
            throw new CVNotFoundException("Profile does not have a CV linked to it.");
        }

        User user = profile.getUser();

        if (!emailProviderService.canUserSendEmails(user)) {
            throw new RuntimeException(
                    "Email provider connection required. Please connect your email account " +
                            "to send job applications with CV attachments."
            );
        }

        byte[] cvData = cvService.downloadCV(profile.getCv().getId(), userId);
        JobApplicationEmailRequest emailRequest = new JobApplicationEmailRequest();
        emailRequest.setToEmail(application.getJobEmail());
        emailRequest.setSubject(application.getSubject());
        emailRequest.setBodyText(formatCoverLetterForEmail(application.getBodyText()));
        emailRequest.setCvData(cvData);
        emailRequest.setCvFileName(profile.getCv().getFileName());
        emailRequest.setCvContentType(profile.getCv().getContentType());

        JobApplication jobApplication = jobApplicationMapper.toEntity(application);
        jobApplication.setProfile(profile);
        jobApplication.setSentAt(LocalDateTime.now());
        jobApplication.setStatus(ApplicationStatus.SENT);

        try {
            emailProviderService.sendJobApplicationEmail(user, emailRequest);
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
     * @param newStatus     The updated status of the application.
     * @param userId        The ID of the user.
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

    private String formatCoverLetterForEmail(String coverLetter) {
        return coverLetter
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replaceAll("\\\\n", "\n")
                .trim();
    }
}