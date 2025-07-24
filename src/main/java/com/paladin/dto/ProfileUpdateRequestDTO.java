package com.paladin.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProfileUpdateRequestDTO {
    private String title;
    private String summary;
    private List<String> skills;
    private UUID cvId;
}
