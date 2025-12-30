package com.paladin.user.service;


import com.paladin.common.dto.UserDTO;
import com.paladin.common.mappers.UserMapper;
import com.paladin.profile.Profile;
import com.paladin.s3_CV_Storage.S3CVStorageService;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.paladin.common.utils.FileUtils.extractKeyFromUrl;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final S3CVStorageService s3CVStorageService;


    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        return userMapper.toDTO(user);
    }


    /**
     * Properly delete user with all associated data
     * This will also delete CVs from S3 storage
     */
    @Transactional
    public void deleteUserCompletely(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Deleting user {} with {} profiles", user.getEmail(), user.getProfiles().size());

        // Delete CVs from S3 before deleting user (to avoid cascade issues)
        for (Profile profile : user.getProfiles()) {
            if (profile.getCv() != null) {
                try {
                    String key = extractKeyFromUrl(profile.getCv().getUrl());
                    s3CVStorageService.deleteFile(key);
                    log.info("Deleted CV from S3: {}", key);
                } catch (Exception e) {
                    log.warn("Failed to delete CV from S3: {}", e.getMessage());
                    // Continue with deletion even if S3 cleanup fails
                }
            }
        }

        // Delete user (cascade will handle profiles, CVs, and job applications)
        userRepository.delete(user);
        log.info("User {} deleted successfully", user.getEmail());
    }

    /**
     * Delete user by email - useful for testing
     */
    @Transactional
    public void deleteUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        deleteUserCompletely(user.getId());
    }
}