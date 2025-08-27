package com.paladin.dto;

import com.paladin.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class JobApplicationDTO {
    private UUID id;
    private String company;
    private String jobEmail;
    private String jobTitle;
    private String profile;
    private ApplicationStatus status;
    private LocalDateTime sentAt;
}
