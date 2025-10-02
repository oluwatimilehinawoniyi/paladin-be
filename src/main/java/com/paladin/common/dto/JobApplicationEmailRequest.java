package com.paladin.common.dto;

import lombok.Data;

@Data
public class JobApplicationEmailRequest {
    private String toEmail;
    private String subject;
    private String bodyText;
    private byte[] cvData;
    private String cvFileName;
    private String cvContentType;
}
