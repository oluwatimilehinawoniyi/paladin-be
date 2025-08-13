package com.paladin.profile.controller;

import com.paladin.dto.*;
import com.paladin.exceptions.UserNotFoundException;
import com.paladin.profile.service.impl.ProfileServiceImpl;
import com.paladin.response.ResponseHandler;
import com.paladin.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> createProfile(
            @RequestParam("title") String title,
            @RequestParam("summary") String summary,
            @RequestParam("skills") String skillsJson,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        log.info("Received file: {} with size: {} bytes and content type: {}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        UUID userId = getUserIdFromPrincipal(principal);
        ProfileCreateRequestDTO request = new ProfileCreateRequestDTO();
        request.setTitle(title);
        request.setSummary(summary);
        request.setFile(file);

        try {
            String[] skillsArray = skillsJson.replace("[", "").replace("]", "")
                    .replace("\"", "").split(",");
            List<String> skills = Arrays.stream(skillsArray)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            request.setSkills(skills);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid skills format"));
        }

        ProfileResponseDTO newProfile =
                profileServiceImpl.createProfileWithCV(request, userId);
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
                HttpStatus.OK, Map.of("success", true)
        );
    }

    private UUID getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized: No principal found");
        }

        if (principal instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            String userEmail = oauth2User.getAttribute("email");

            if (userEmail == null) {
                throw new RuntimeException("Email not found in OAuth2 user attributes");
            }

            UserDTO user = userService.getUserByEmail(userEmail);
            if (user == null) {
                throw new UserNotFoundException(
                        "User not found for authenticated email: " + userEmail);
            }
            return user.getId();
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
