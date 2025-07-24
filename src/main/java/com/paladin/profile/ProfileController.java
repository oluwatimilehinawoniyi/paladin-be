package com.paladin.profile;

import com.paladin.dto.*;
import com.paladin.exceptions.UserNotFoundException;
import com.paladin.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<List<ProfileSummaryDTO>> getProfiles(
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        List<ProfileSummaryDTO> profiles =
                profileService.getProfilesByUserId(userId);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileResponseDTO> getProfileById(
            @PathVariable UUID profileId, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        ProfileResponseDTO profile =
                profileService.getProfileById(profileId, userId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<ProfileResponseDTO> createProfile(
            @Valid @RequestBody ProfileCreateRequestDTO request,
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        ProfileResponseDTO newProfile =
                profileService.createProfileForUser(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(newProfile);
    }

    @PatchMapping("/{profileId}")
    public ResponseEntity<ProfileResponseDTO> updateProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody ProfileUpdateRequestDTO request,
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        ProfileResponseDTO updatedProfile =
                profileService.updateProfile(userId, profileId, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> deleteProfile(
            @PathVariable UUID profileId,
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        profileService.deleteProfile(profileId, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized: No principal found");
        }
        String userEmail = principal.getName();
        UserDTO user = userService.getUserByEmail(userEmail);
        if (user == null) {
            throw new UserNotFoundException(
                    "User not found for authenticated principal: " + userEmail);
        }
        return user.getId();
    }
}
