package com.paladin.jobApplication;

import com.paladin.enums.ApplicationStatus;
import com.paladin.cv.CV;
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

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cv_id", nullable = false)
    private CV cv;
}
