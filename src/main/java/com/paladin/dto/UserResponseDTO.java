package com.paladin.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserResponseDTO {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
}
