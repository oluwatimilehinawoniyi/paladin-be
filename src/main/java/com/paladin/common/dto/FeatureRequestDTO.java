package com.paladin.common.dto;

import com.paladin.common.enums.FeatureRequestCategory;
import com.paladin.common.enums.FeatureRequestStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRequestDTO {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String title;
    private String description;
    private FeatureRequestCategory category;
    private String categoryDisplayName;
    private FeatureRequestStatus status;
    private String statusDisplayName;
    private String adminResponse;
    private Long voteCount;
    private Boolean hasVoted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
