package com.paladin.common.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Data
public class ProfileCreateRequestDTO {

    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Summary cannot be empty")
    private String summary;

    @NotNull(message = "A cv must be uploaded when creating a profile")
    private MultipartFile file;

    private List<String> skills;
}