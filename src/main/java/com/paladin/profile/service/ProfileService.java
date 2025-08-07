package com.paladin.profile.service;

import com.paladin.dto.ProfileCreateRequestDTO;
import com.paladin.dto.ProfileResponseDTO;
import com.paladin.dto.ProfileSummaryDTO;
import com.paladin.dto.ProfileUpdateRequestDTO;

import java.util.List;
import java.util.UUID;

public interface ProfileService {
    public ProfileResponseDTO createProfileForUser(
            ProfileCreateRequestDTO request,
            UUID userId);

    public List<ProfileSummaryDTO> getProfilesByUserId(UUID userId);

    public ProfileResponseDTO getProfileById(UUID profileId, UUID userId);

    public ProfileResponseDTO updateProfile(
            UUID userId,
            UUID profileId,
            ProfileUpdateRequestDTO request);

    public void deleteProfile(UUID profileId, UUID userId);
}
