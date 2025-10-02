package com.paladin.jobApplication.controller;

import com.paladin.common.dto.*;
import com.paladin.common.enums.ApplicationStatus;
import com.paladin.common.exceptions.UserNotFoundException;
import com.paladin.jobApplication.service.impl.AIJobAnalysisServiceImpl;
import com.paladin.jobApplication.service.impl.JobApplicationServiceImpl;
import com.paladin.common.response.ResponseHandler;
import com.paladin.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    private final AIJobAnalysisServiceImpl aiJobAnalysisService;


    @PostMapping("/send")
    public ResponseEntity<Object> sendJobApplication(
            @RequestBody NewJobApplicationDTO request,
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);

        JobApplicationDTO createdApplication =
                jobApplicationServiceImpl.createAndSendJobApplication(
                        request,
                        userId
                );
        return ResponseHandler.responseBuilder("Application successfully sent",
                HttpStatus.OK, createdApplication);
    }

    @GetMapping("/me")
    public ResponseEntity<Object> getMyJobApplications(
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);

        List<JobApplicationDTO> applications =
                jobApplicationServiceImpl.getJobApplicationsByUserId(
                        userId);
        return ResponseHandler.responseBuilder(
                "Job applications successfully returned",
                HttpStatus.OK,
                applications);
    }

    @PostMapping("/analyze-application")
    public ResponseEntity<Object> analyzeSmartApplication(
            @RequestBody SmartAnalysisRequest request,
            Principal principal
    ) {
        UUID userId = getUserIdFromPrincipal(principal);

        AIJobAnalysisResponse response = aiJobAnalysisService.analyseJobApplication(request, userId);
        return ResponseHandler.responseBuilder(
                "Job applications successfully analysed",
                HttpStatus.OK,
                response);
    }

    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<Object> updateApplicationStatus(
            @PathVariable UUID applicationId,
            @RequestBody ApplicationStatus newStatus,
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);

        JobApplicationDTO updatedApplication =
                jobApplicationServiceImpl.updateJobApplicationStatus(
                        applicationId, newStatus, userId);
        return ResponseHandler.responseBuilder(
                "Job application status successfully updated",
                HttpStatus.OK,
                updatedApplication);
    }

    // Helper method to get user id
    private UUID getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized: No principal found");
        }

        if (principal instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            String userEmail = oauth2User.getAttribute("email");

            if (userEmail == null) {
                throw new RuntimeException("Email not found in OAuth2 user attributes");
            }

            UserDTO user = userService.getUserByEmail(userEmail);
            if (user == null) {
                throw new UserNotFoundException(
                        "User not found for authenticated email: " + userEmail);
            }
            return user.getId();
        }

        String userEmail = principal.getName();
        UserDTO user = userService.getUserByEmail(userEmail);
        if (user == null) {
            throw new UserNotFoundException(
                    "User not found for authenticated principal: " + userEmail);
        }
        return user.getId();
    }
}