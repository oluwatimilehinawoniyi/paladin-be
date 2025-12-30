package com.paladin.common.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SmartAnalysisRequest {
    private UUID profileId;
    private String jobDescription;
}
