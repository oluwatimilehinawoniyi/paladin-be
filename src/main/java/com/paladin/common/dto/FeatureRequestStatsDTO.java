package com.paladin.common.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRequestStatsDTO {
    private Long total;
    private Long pending;
    private Long underReview;
    private Long inProgress;
    private Long completed;
    private Long rejected;
}
