package com.paladin.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProfileCreateRequestDTO {

    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Summary cannot be empty")
    private String summary;

    private List<String> skills;

    private UUID cvId;
}