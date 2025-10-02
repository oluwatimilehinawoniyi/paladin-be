package com.paladin.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AIJobAnalysisResponse {
    private JobDetailsDTO jobDetails;

    private String coverLetter;

    private JobMatchAnalysisDTO matchAnalysis;
}