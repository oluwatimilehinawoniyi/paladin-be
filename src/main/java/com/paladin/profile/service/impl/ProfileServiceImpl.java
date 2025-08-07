package com.paladin.profile.service.impl;

import com.paladin.cv.CV;
import com.paladin.cv.repository.CVRepository;
import com.paladin.dto.*;
import com.paladin.exceptions.CVNotFoundException;
import com.paladin.exceptions.UserNotFoundException;
import com.paladin.mappers.ProfileMapper;
import com.paladin.profile.Profile;
import com.paladin.profile.repository.ProfileRepository;
import com.paladin.profile.service.ProfileService;
import com.paladin.s3_CV_Storage.S3CVStorageService;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.paladin.utils.FileUtils.extractKeyFromUrl;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final CVRepository cVRepository;
    private final S3CVStorageService s3CVStorageService;
    private final UserRepository userRepository;

    // create profile
    @Transactional
    public ProfileResponseDTO createProfileForUser(
            ProfileCreateRequestDTO request,
            UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found with ID: " + userId));

        Profile newProfile = profileMapper.toEntity(request);
        newProfile.setCreatedAt(LocalDateTime.now());
        newProfile.setUser(user);

        if (request.getCvId() != null) {
            CV cv = cVRepository.findById(request.getCvId())
                    .orElseThrow(() -> new CVNotFoundException(
                            "CV not found with ID: " + request.getCvId()));
            newProfile.setCv(cv);
        }

        Profile savedProfile = profileRepository.save(newProfile);
        return profileMapper.toResponseDTO(savedProfile);
    }

    // read profiles
    public List<ProfileSummaryDTO> getProfilesByUserId(UUID userId) {
        List<Profile> profiles = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Profile " +
                        "not found"));
        return profiles.stream()
                .map(profileMapper::toSummaryDTO)
                .toList();
    }

    // get profile by ID
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

    // update profile
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

    // delete profile
    @Transactional
    public void deleteProfile(UUID profileId, UUID userId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not " +
                        "found"));
        if (!profile.getUser().getId().equals(userId)) {
            throw new RuntimeException(
                    "Unauthorized: This profile does not belong to you");
        }

        if (profile.getCv() != null) {
            String key = extractKeyFromUrl(profile.getCv().getUrl());
            s3CVStorageService.deleteFile(key);
            cVRepository.delete(profile.getCv());
            profile.setCv(null);
        }

        profileRepository.delete(profile);
    }
}

