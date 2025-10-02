package com.paladin.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BuildComprehensivePrompt {

    public static String prompt(String title, String cvText, String jobDescription) {
        return """
            Analyze this job application scenario and provide comprehensive analysis.
            
            CANDIDATE PROFILE:
            Title: %s
            
            CV CONTENT:
            ---
            %s
            ---
            
            JOB DESCRIPTION:
            %s
            
            PROVIDE ANALYSIS IN THIS EXACT JSON FORMAT:
            {
              "jobDetails": {
                "company": "extracted company name from job description",
                "position": "job title from JD",
                "email": "contact email if found in JD, otherwise null",
                "requirements": ["key requirement 1", "key requirement 2", "etc"],
                "keySkills": ["required skill 1", "required skill 2", "etc"],
                "experienceLevel": "exact experience requirement from JD (e.g., '3+ years', 'Entry level')",
                "location": "job location if mentioned, otherwise null"
              },
              "coverLetter": "Write a powerful cover letter in exactly 3 paragraphs: (1) Opening with specific role interest and top qualification match, (2) 2-3 concrete examples of relevant experience from CV that address job requirements, (3) Strong closing with enthusiasm and call to action mentioning CV attachment. Maximum 180 words total. Address to 'Dear Hiring Manager' and sign with 'Best regards, [candidate full name from CV]'",
              "matchAnalysis": {
                "overallMatchPercentage": "Calculate actual percentage (0-100) based on skills match, experience alignment, and requirements fulfillment",
                "matchingSkills": ["skills from CV that match JD requirements"],
                "missingSkills": ["skills required in JD but NOT found in candidate's CV"],
                "strengths": ["specific strengths from CV that align with job"],
                "weaknesses": ["areas where candidate might be lacking"],
                "recommendation": "brief recommendation about applying (100-150 words)",
                "confidenceLevel": "High/Medium/Low based on overall match"
              }
            }
            
            IMPORTANT INSTRUCTIONS:
            - Extract company name, position, and email accurately from the job description
            - Generate a compelling cover letter that specifically references both CV content and job requirements
            - Calculate match percentage based on skills alignment, experience level, and requirements
            - Be honest about missing skills but highlight transferable skills
            - Use candidate's actual full name from CV in signature
            - Provide actionable recommendations
            - Return ONLY the JSON response, no additional text
            
            CRITICAL JSON FORMATTING:
            - Return valid JSON only
            - Use \\n for line breaks in cover letter text
            - Properly escape quotes and special characters
            - Ensure all JSON syntax is correct
            - No markdown formatting around JSON
            """.formatted(title, cvText, jobDescription);
    }
}