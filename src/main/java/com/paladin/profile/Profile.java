package com.paladin.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.paladin.cv.CV;
import com.paladin.jobApplication.JobApplication;
import com.paladin.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
public class Profile {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String summary;

    @ElementCollection
    private List<String> skills = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user = new User();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cv_id")
    private CV cv;

    @OneToMany(mappedBy = "profile",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    private List<JobApplication> jobApplications = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
