package com.paladin.jobApplication.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.paladin.common.dto.*;
import com.paladin.cv.service.impl.CVServiceImpl;
import com.paladin.common.exceptions.CVNotFoundException;
import com.paladin.profile.service.ProfileService;
import com.paladin.common.utils.BuildComprehensivePrompt;
import com.paladin.common.utils.PDFTextExtractor;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIJobAnalysisServiceImpl {

    private final ProfileService profileService;
    private final CVServiceImpl cvService;
    private final ChatClient chatClient;
    private final PDFTextExtractor pdfTextExtractor;
    private final ObjectMapper objectMapper;

    /**
     * Analyses a profile and job description to extract necessary information for job application and check the level of job match
     *
     * @param request A DTO that contains the profileId and job description
     * @return A cover letter, necessary company information and job match analysis
     */
    public AIJobAnalysisResponse analyseJobApplication(SmartAnalysisRequest request, UUID userId) {
        try {
            ProfileResponseDTO profile = profileService.getProfileById(request.getProfileId(), userId);

            if (profile.getCv() == null) {
                throw new CVNotFoundException("No CV found for this profile");
            }

            log.info("Downloading CV for analysis: {}", profile.getCv().getFileName());
            byte[] cvBytes = cvService.downloadCV(profile.getCv().getId(), userId);

            log.info("Extracting text from CV...");
            String cvText = pdfTextExtractor.getText(cvBytes);

            String prompt = BuildComprehensivePrompt.prompt(
                    profile.getTitle(),
                    cvText,
                    request.getJobDescription()
            );

            // Call Claude AI with retry logic
            String aiResponse = callClaudeAIWithRetry(prompt);

            log.debug("Raw AI Response received: {}", aiResponse);
            return parseAIResponse(aiResponse);
        } catch (CVNotFoundException e) {
            log.error("CV not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error during AI job analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze job application: " + e.getMessage(), e);
        }
    }

    @CircuitBreaker(name = "claudeAIService", fallbackMethod = "fallbackClaudeAI")
    @Retryable(
            retryFor = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 5000
            )
    )
    private String callClaudeAIWithRetry(String prompt) {
        log.info("Calling Claude AI API (with circuit breaker + retry)...");
        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.info("Claude AI responded successfully");
            return response;
        } catch (Exception e) {
            log.warn("Claude AI call failed: {} - Will retry...", e.getMessage());
            throw new RuntimeException("Claude AI API call failed", e);
        }
    }

    /**
     * Circuit breaker fallback - called when Claude AI circuit is OPEN.
     * Returns a template response so user can still send their application.
     */
    private String fallbackClaudeAI(String prompt, Exception e) {
        log.error("Circuit breaker OPEN for Claude AI service");
        log.error("Reason: {}", e.getMessage());

        // PRODUCTION READY: Return template cover letter that user can customize
        log.info("Returning template response - user can still send application");

        // TODO for future:
        // 1. Check cache for similar job descriptions
        // 2. Queue request for manual processing
        // 3. Send in-app notification to user

        return """
                {
                    "jobDetails": {
                        "company": "Please review manually",
                        "position": "Review job description"
                    },
                    "coverLetter": "Dear Hiring Manager,\\n\\nI am writing to express my strong interest in this position. My background and professional experience align well with the requirements outlined in the job description.\\n\\nPlease review my attached CV for detailed information about my skills, experience, and accomplishments. I am confident that my qualifications make me a strong candidate for this role.\\n\\nI would welcome the opportunity to discuss how I can contribute to your organization. Thank you for considering my application.\\n\\nBest regards",
                    "matchAnalysis": {
                        "overallMatchPercentage": null,
                        "matchingSkills": [],
                        "missingSkills": [],
                        "recommendation": "AI analysis temporarily unavailable. This is a template cover letter that you can customize. Please review the job description carefully and edit the cover letter to highlight your most relevant skills and experience.",
                        "confidenceLevel": "Template Response"
                    }
                }
                """;
    }

    /**
     * Recovery method when Claude AI fails after all retries.
     * Logs the failure and throws a descriptive exception for the controller to handle.
     */
    @Recover
    private String recoverFromAIFailure(RuntimeException e, String prompt) {
        log.error("Claude AI failed after all retries: {}", e.getMessage());
        log.error("AI service is temporarily unavailable. Manual processing may be required.");

        // TODO:
        // 1. Store the failed request for manual processing
        // 2. Send notification to ops team about AI service degradation
        // 3. Track failure metrics for monitoring/alerting

        throw new RuntimeException(
                "AI analysis service is temporarily unavailable. Please try again later or contact support for manual processing.",
                e
        );
    }

    private AIJobAnalysisResponse parseAIResponse(String aiResponse) {
        try {
            log.info("Parsing AI response to DTO");
            log.debug("AI Response content: {}", aiResponse);

            String cleanJson = aiResponse.trim();
            return objectMapper.readValue(cleanJson, AIJobAnalysisResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            log.debug("AI Response was: {}", aiResponse);

            // return error response
            return AIJobAnalysisResponse.builder()
                    .jobDetails(JobDetailsDTO.builder()
                            .company("Unable to extract")
                            .position("Unable to extract")
                            .build())
                    .coverLetter("Sorry, we couldn't generate a cover letter due to a parsing error. Please try again.")
                    .matchAnalysis(JobMatchAnalysisDTO.builder()
                            .overallMatchPercentage(0)
                            .recommendation("Analysis failed. Please try again.")
                            .confidenceLevel("Low")
                            .build())
                    .build();
        }

    }


}
