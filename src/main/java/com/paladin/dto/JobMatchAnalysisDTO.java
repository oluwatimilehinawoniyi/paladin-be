package com.paladin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JobMatchAnalysisDTO {
    private Integer overallMatchPercentage;        // 75%
    private List<String> matchingSkills;           // ["React", "Node.js"]
    private List<String> missingSkills;            // ["Docker", "AWS"]
    private List<String> strengths;                // ["5+ years React experience"]
    private List<String> weaknesses;               // ["No cloud experience mentioned"]
    private String recommendation;                 // "Strong match, consider highlighting..."
    private String confidenceLevel;                // "High", "Medium", "Low"
}