package com.paladin.common.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProfileResponseDTO {
    private UUID id;
    private String title;
    private String summary;
    private UUID userId;
    private List<String> skills;
    private CVDTO cv;
    private LocalDateTime createdAt;
}
