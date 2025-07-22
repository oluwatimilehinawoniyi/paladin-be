package com.paladin.profile;

import com.paladin.dto.ProfileDTO;
import com.paladin.dto.ProfileSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private static final UUID HARDCODED_USER_ID = UUID.fromString("7d77831d-d0d4-4e66-aaa9-20c160a6b7d7");

    @GetMapping("/me")
    public List<ProfileSummaryDTO> getProfiles() {
        return profileService.getProfilesByUserId(HARDCODED_USER_ID);
    }

    @PostMapping
    public ProfileDTO createProfile(
            @RequestBody ProfileDTO dto) {
        return profileService.createProfileForUser(dto, HARDCODED_USER_ID);
    }

    @PatchMapping("/{id}")
    public ProfileDTO updateProfile(
            @RequestBody ProfileDTO dto,
            @PathVariable UUID id) {
        return profileService.updateProfile(HARDCODED_USER_ID, id, dto);
    }

    @DeleteMapping("/{profileId}")
    public void deleteProfile(@PathVariable UUID profileId) {
        profileService.deleteProfile(profileId, HARDCODED_USER_ID);
    }
}
