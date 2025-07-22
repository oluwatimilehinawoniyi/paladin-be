package com.paladin.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProfileSummaryDTO {
    private UUID id;
    private String title;
    private String summary;
    private CVSummaryDTO cv;
    private LocalDateTime createdAt;
}
