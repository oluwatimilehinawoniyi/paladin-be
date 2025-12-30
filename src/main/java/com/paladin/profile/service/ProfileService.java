package com.paladin.profile.service;

import com.paladin.common.dto.ProfileCreateRequestDTO;
import com.paladin.common.dto.ProfileResponseDTO;
import com.paladin.common.dto.ProfileSummaryDTO;
import com.paladin.common.dto.ProfileUpdateRequestDTO;

import java.util.List;
import java.util.UUID;

public interface ProfileService {
    public ProfileResponseDTO createProfileWithCV(
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
