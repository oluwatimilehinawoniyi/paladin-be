package com.paladin.cv;

import com.paladin.s3_CV_Storage.S3CVStorageService;
import com.paladin.common.utils.ApplicationContextProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.paladin.common.utils.FileUtils.extractKeyFromUrl;

@Entity
@Data
@Slf4j
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class CV {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @PreRemove
    public void preRemove() {
        try {
            if (url != null) {
                S3CVStorageService s3Service = ApplicationContextProvider.getBean(S3CVStorageService.class);
                String key = extractKeyFromUrl(url);
                s3Service.deleteFile(key);
                log.info("Successfully deleted CV from S3: {}", key);
            }
        } catch (Exception e) {
            log.error("Failed to delete CV from S3 during cascade delete: {}", e.getMessage());
        }
    }
}
