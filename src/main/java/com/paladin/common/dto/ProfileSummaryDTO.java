package com.paladin.common.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ProfileSummaryDTO {
    private UUID id;
    private String title;
    private String summary;
    private List<String> skills;
    private CVSummaryDTO cv;
    private LocalDateTime createdAt;
}
