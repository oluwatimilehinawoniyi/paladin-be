package com.paladin.profile;

import com.paladin.cv.CV;
import com.paladin.cv.CVRepository;
import com.paladin.dto.ProfileDTO;
import com.paladin.dto.ProfileSummaryDTO;
import com.paladin.dto.UserDTO;
import com.paladin.exceptions.CVNotFoundException;
import com.paladin.mappers.ProfileMapper;
import com.paladin.mappers.UserMapper;
import com.paladin.s3_CV_Storage.S3CVStorageService;
import com.paladin.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.paladin.utils.FileUtils.extractKeyFromUrl;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final UserService userService;
    private final UserMapper userMapper;
    private final CVRepository cVRepository;
    private final S3CVStorageService s3CVStorageService;

    // create profile
    public ProfileDTO createProfileForUser(ProfileDTO profile,
                                           UUID userId) {
        Profile newProfile = profileMapper.toEntity(profile);
        UserDTO user = userService.getUserById(userId);
        newProfile.setCreatedAt(LocalDateTime.now());
        newProfile.setUser(userMapper.toEntity(user));

        return profileMapper.toDTO(profileRepository.save(newProfile));
    }

    // read profiles
    public List<ProfileSummaryDTO> getProfilesByUserId(UUID userId) {
        List<Profile> profiles = profileRepository.findByUserId(userId);
        return profiles.stream()
                .map(profileMapper::toSummaryDTO)
                .toList();
    }


    // update profile
    public ProfileDTO updateProfile(
            UUID userId,
            UUID profileId,
            ProfileDTO dto) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(
                        () -> new RuntimeException("Profile not found"));

        if (!profile.getUser().getId().equals(userId)) {
            throw new RuntimeException(
                    "Unauthorized: This profile does not belong to you");
        }

        profileMapper.updateProfileFromDto(dto, profile);

        if (dto.getCv() != null && dto.getCv().getId() != null) {
            CV cv =
                    cVRepository.findById(dto.getCv().getId()).orElseThrow(
                            () -> new CVNotFoundException("CV not found"));
            profile.setCv(cv);
        }

        return profileMapper.toDTO(profileRepository.save(profile));
    }

    // delete profile
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
        }

        profileRepository.delete(profile);
    }
}

