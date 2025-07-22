package com.paladin.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
}
