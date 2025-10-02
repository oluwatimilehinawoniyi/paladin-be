package com.paladin.common.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CVSummaryDTO {
    private UUID id;
    private String fileName;
    private String url;
    private long size;
    private LocalDateTime uploadedAt;
}