package com.paladin.featureRequest;

import com.paladin.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "feature_request_vote",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"feature_request_id", "user_id"}
        ))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRequestVote {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_request_id", nullable = false)
    private FeatureRequest featureRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}