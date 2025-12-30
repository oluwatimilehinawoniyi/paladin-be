package com.paladin.profile.service.impl;

import com.paladin.common.dto.*;
import com.paladin.common.exceptions.NotFoundException;
import com.paladin.cv.CV;
import com.paladin.cv.repository.CVRepository;
import com.paladin.cv.service.impl.CVServiceImpl;
import com.paladin.common.exceptions.CVNotFoundException;
import com.paladin.common.mappers.ProfileMapper;
import com.paladin.profile.Profile;
import com.paladin.profile.repository.ProfileRepository;
import com.paladin.profile.service.ProfileService;
import com.paladin.s3_CV_Storage.S3CVStorageService;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final CVRepository cVRepository;
    private final S3CVStorageService s3CVStorageService;
    private final UserRepository userRepository;
    private final CVServiceImpl cvService;

    /**
     * Creates a profile with CV attached.
     *
     * @param request The DTO containing the fields to create the profile.
     * @param userId  The ID of the user.
     * @return The created profile.
     */
    @Transactional
    public ProfileResponseDTO createProfileWithCV(
            ProfileCreateRequestDTO request,
            UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        Profile newProfile = profileMapper.toEntity(request);
        newProfile.setCreatedAt(LocalDateTime.now());
        newProfile.setUser(user);

        // save profile to get ID
        Profile savedProfile = profileRepository.save(newProfile);

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            CVDTO cvdto = cvService.uploadCV(request.getFile(), savedProfile.getId(), userId);
            CV cv = cvService.getCVByIdAsEntity(UUID.fromString(cvdto.getId().toString()));
            savedProfile.setCv(cv);
            savedProfile = profileRepository.save(savedProfile);
        }

        return profileMapper.toResponseDTO(savedProfile);
    }

    /**
     * Fetches the profiles created by a user.
     *
     * @param userId The ID of the user.
     * @return The list of profiles created by the user.
     */
    public List<ProfileSummaryDTO> getProfilesByUserId(UUID userId) {
        List<Profile> profiles = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profile " +
                        "not found"));
        return profiles.stream()
                .map(profileMapper::toSummaryDTO)
                .toList();
    }

    /**
     * Fetches a single profile created by a user.
     *
     * @param userId The ID of the user.
     * @return The profile created by the user.
     */
    public ProfileResponseDTO getProfileById(UUID profileId, UUID userId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException(
                        "Profile not found with ID: " + profileId));

        if (!profile.getUser().getId().equals(userId)) {
            throw new RuntimeException(
                    "Unauthorized: This profile does not belong to you");
        }
        return profileMapper.toResponseDTO(profile);
    }

    /**
     * Updates a profile of a user.
     *
     * @param userId The ID of the user.
     * @param profileId The ID of the profile to be edited.
     * @return The updated profile.
     */
    @Transactional
    public ProfileResponseDTO updateProfile(
            UUID userId,
            UUID profileId,
            ProfileUpdateRequestDTO request) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(
                        () -> new RuntimeException("Profile not found"));

        if (!profile.getUser().getId().equals(userId)) {
            throw new RuntimeException(
                    "Unauthorized: This profile does not belong to you");
        }

        profileMapper.updateProfileFromDto(request, profile);

        if (request.getCvId() != null) {
            CV cv =
                    cVRepository.findById(request.getCvId()).orElseThrow(
                            () -> new CVNotFoundException("CV not found"));
            profile.setCv(cv);
        }

        Profile updatedProfile = profileRepository.save(profile);
        return profileMapper.toResponseDTO(updatedProfile);
    }

    /**
     * Deletes a profile of a user.
     *
     * @param userId The ID of the user.
     * @param profileId The ID of the profile to be deleted.
     */
    @Transactional
    public void deleteProfile(UUID profileId, UUID userId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not " +
                        "found"));
        if (!profile.getUser().getId().equals(userId)) {
            throw new RuntimeException(
                    "Unauthorized: This profile does not belong to you");
        }

        profileRepository.delete(profile);
        log.info("Profile {} deleted successfully", profileId);
    }
}

