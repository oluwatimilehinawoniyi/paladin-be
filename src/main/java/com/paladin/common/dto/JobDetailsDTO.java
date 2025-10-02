package com.paladin.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JobDetailsDTO {
    private String company;
    private String position;
    private String email;
    private List<String> requirements;
    private List<String> keySkills;
    private String experienceLevel;
}
