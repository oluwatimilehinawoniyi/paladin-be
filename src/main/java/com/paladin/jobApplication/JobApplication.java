package com.paladin.jobApplication;

import com.paladin.common.enums.ApplicationStatus;
import com.paladin.profile.Profile;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class JobApplication {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String jobEmail;

    @Column(nullable = false)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.SENT;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;
}
