package com.paladin.common.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class NewJobApplicationDTO {
    public String company;
    public String jobEmail;
    public String jobTitle;
    public String subject;
    public String bodyText;
    public UUID profileId;
}
