package com.paladin.profile.controller;

import com.paladin.dto.*;
import com.paladin.exceptions.UserNotFoundException;
import com.paladin.profile.service.impl.ProfileServiceImpl;
import com.paladin.response.ResponseHandler;
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

    private final ProfileServiceImpl profileServiceImpl;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Object> getProfiles(
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        List<ProfileSummaryDTO> profiles =
                profileServiceImpl.getProfilesByUserId(userId);

        return ResponseHandler.responseBuilder(
                "List of profiles for the current user",
                HttpStatus.OK,
                profiles
        );
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<Object> getProfileById(
            @PathVariable UUID profileId, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        ProfileResponseDTO profile =
                profileServiceImpl.getProfileById(profileId, userId);
        return ResponseHandler.responseBuilder(
                "Details of the requested profile for the current user",
                HttpStatus.OK,
                profile
        );
    }

    @PostMapping
    public ResponseEntity<Object> createProfile(
            @Valid @RequestBody ProfileCreateRequestDTO request,
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        ProfileResponseDTO newProfile =
                profileServiceImpl.createProfileForUser(request, userId);
        return ResponseHandler.responseBuilder(
                "Profile successfully created",
                HttpStatus.CREATED,
                newProfile);
    }

    @PatchMapping("/{profileId}")
    public ResponseEntity<Object> updateProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody ProfileUpdateRequestDTO request,
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        ProfileResponseDTO updatedProfile =
                profileServiceImpl.updateProfile(userId, profileId, request);
        return ResponseHandler.responseBuilder(
                "Profile successfully updated",
                HttpStatus.OK,
                updatedProfile);
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<Object> deleteProfile(
            @PathVariable UUID profileId,
            Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        profileServiceImpl.deleteProfile(profileId, userId);
        return ResponseHandler.responseBuilder(
                "Profile successfully deleted",
                HttpStatus.NO_CONTENT, ""
        );
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
