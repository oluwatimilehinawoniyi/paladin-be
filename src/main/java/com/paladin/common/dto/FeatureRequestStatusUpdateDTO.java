package com.paladin.common.dto;

import com.paladin.common.enums.FeatureRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRequestStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private FeatureRequestStatus status;

    private String adminResponse;
}