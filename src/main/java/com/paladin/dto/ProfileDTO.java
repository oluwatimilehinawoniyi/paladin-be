package com.paladin.dto;

import com.paladin.cv.CV;
import com.paladin.user.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ProfileDTO {
    private UUID id;
    private String title;
    private String summary;
    private UUID userId;
    private List<String> skills;
//    private CVSummaryDTO cv;
    private CV cv;
    private LocalDateTime createdAt;
}
