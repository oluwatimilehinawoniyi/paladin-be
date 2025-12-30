package com.paladin.jobApplication.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.paladin.common.dto.*;
import com.paladin.cv.service.impl.CVServiceImpl;
import com.paladin.common.exceptions.CVNotFoundException;
import com.paladin.profile.service.ProfileService;
import com.paladin.common.utils.BuildComprehensivePrompt;
import com.paladin.common.utils.PDFTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

            log.info("Sending CV and job description to Claude for analysis");
            String aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("Raw AI Response received: {}", aiResponse);
            return parseAIResponse(aiResponse);
        } catch (Exception e) {
            log.error("Error during AI job analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze job application: " + e.getMessage(), e);
        }
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
