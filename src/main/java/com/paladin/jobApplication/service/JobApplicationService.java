package com.paladin.jobApplication.service;

import com.paladin.common.dto.JobApplicationDTO;
import com.paladin.common.dto.NewJobApplicationDTO;
import com.paladin.common.enums.ApplicationStatus;

import java.util.List;
import java.util.UUID;

public interface JobApplicationService {
    public JobApplicationDTO createAndSendJobApplication(
            NewJobApplicationDTO dto,
            UUID userId);

    public List<JobApplicationDTO> getJobApplicationsByUserId(UUID userId);

    public JobApplicationDTO updateJobApplicationStatus(UUID applicationId, ApplicationStatus newStatus, UUID userId);
}
