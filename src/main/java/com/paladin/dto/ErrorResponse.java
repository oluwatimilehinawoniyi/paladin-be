package com.paladin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String message;
    private LocalDateTime timestamp;
    private String path;
    private int status;

    public ErrorResponse(String message, String path, int status) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.path = path;
        this.status = status;
    }
}
