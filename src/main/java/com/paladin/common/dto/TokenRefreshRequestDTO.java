package com.paladin.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for token refresh requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequestDTO {
    private String refreshToken;
}
