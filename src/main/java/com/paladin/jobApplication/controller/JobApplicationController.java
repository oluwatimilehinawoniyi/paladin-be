package com.paladin.jobApplication.controller;

import com.paladin.dto.JobApplicationDTO;
import com.paladin.dto.NewJobApplicationDTO;
import com.paladin.enums.ApplicationStatus;
import com.paladin.jobApplication.service.impl.JobApplicationServiceImpl;
import com.paladin.response.ResponseHandler;
import com.paladin.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/job-applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationServiceImpl jobApplicationServiceImpl;
    private final UserService userService;

    @PostMapping("/send")
    public ResponseEntity<Object> sendJobApplication(
            @RequestBody NewJobApplicationDTO request,
            Principal principal) {
        UUID currentUserId = getCurrentUserId(principal);

        JobApplicationDTO createdApplication =
                jobApplicationServiceImpl.createAndSendJobApplication(
                        request,
                        currentUserId
                );
        return ResponseHandler.responseBuilder("Application successfully sent",
                HttpStatus.OK, createdApplication);
    }

    @GetMapping("/me")
    public ResponseEntity<Object> getMyJobApplications(
            Principal principal) {
        UUID currentUserId = getCurrentUserId(principal);

        List<JobApplicationDTO> applications =
                jobApplicationServiceImpl.getJobApplicationsByUserId(
                        currentUserId);
        return ResponseHandler.responseBuilder(
                "Job applications successfully returned",
                HttpStatus.OK,
                applications);
    }

    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<Object> updateApplicationStatus(
            @PathVariable UUID applicationId,
            @RequestBody ApplicationStatus newStatus,
            Principal principal) {
        UUID currentUserId = getCurrentUserId(principal);

        JobApplicationDTO updatedApplication =
                jobApplicationServiceImpl.updateJobApplicationStatus(
                        applicationId, newStatus, currentUserId);
        return ResponseHandler.responseBuilder(
                "Job application status successfully updated",
                HttpStatus.OK,
                updatedApplication);
    }

    // Helper method to get user id
    private UUID getCurrentUserId(Principal principal) {
        return userService.getUserByEmail(principal.getName()).getId();
    }
}