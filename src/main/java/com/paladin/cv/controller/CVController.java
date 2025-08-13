package com.paladin.cv.controller;

import com.paladin.cv.service.impl.CVServiceImpl;
import com.paladin.dto.CVDTO;
import com.paladin.dto.UserDTO;
import com.paladin.exceptions.UserNotFoundException;
import com.paladin.response.ResponseHandler;
import com.paladin.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cv")
@Tag(name = "CV Management",
        description = "Endpoints for managing CV uploads")
public class CVController {

    private final CVServiceImpl cvServiceImpl;
    private final UserService userService;

    @Operation(
            summary = "Upload a CV",
            description = "Uploads a CV file to S3 and returns its key",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Upload successful"),
                    @ApiResponse(responseCode = "400",
                            description = "Invalid input")
            }
    )
    @PostMapping("/upload")
    public ResponseEntity<Object> uploadCV(
            @RequestParam("file") MultipartFile file,
            @RequestParam("profileId") UUID profileId,
            Principal principal
    ) {
        UUID userId = getUserIdFromPrincipal(principal);
        CVDTO cvdto = cvServiceImpl.uploadCV(file, profileId, userId);
        return ResponseHandler.responseBuilder(
                "CV successfully updated",
                HttpStatus.OK,
                cvdto);
    }

    @GetMapping("/{cvId}")
    public ResponseEntity<Object> getCVById(
            @PathVariable UUID cvId,
            Principal principal
    ) {
        UUID userId = getUserIdFromPrincipal(principal);
        CVDTO cvdto = cvServiceImpl.getCVById(cvId, userId);
        return ResponseHandler.responseBuilder("CV successfully returned", HttpStatus.OK, cvdto);
    }

    @GetMapping("/profile/{profileId}")
    public ResponseEntity<Object> getCVByProfileId(
            @PathVariable UUID profileId, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        CVDTO cvdto = cvServiceImpl.getCVbyProfileId(profileId, userId);
        return ResponseHandler.responseBuilder(
                "CV successfully returned",
                HttpStatus.OK,
                cvdto);
    }

    @GetMapping("/{cvId}/download")
    public void downloadCV(@PathVariable UUID cvId,
                           HttpServletResponse response,
                           Principal principal)
            throws IOException {
        UUID userId =
                getUserIdFromPrincipal(principal);
        byte[] cvData = cvServiceImpl.downloadCV(cvId, userId);
        CVDTO cvDTO = cvServiceImpl.getCVById(cvId, userId);

        response.setContentType(cvDTO.getContentType());
        response.setContentLength(cvData.length);
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + cvDTO.getFileName() + "\"");

        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(cvData);
        outputStream.flush();
        outputStream.close();
    }

    @PutMapping("/{cvId}")
    public ResponseEntity<Object> updateCV(
            @PathVariable UUID cvId,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        UUID userId = getUserIdFromPrincipal(principal);
        CVDTO updatedCV = cvServiceImpl.updateCV(cvId, file, userId);
        return ResponseHandler.responseBuilder(
                "CV successfully returned",
                HttpStatus.OK,
                updatedCV);
    }

    @DeleteMapping("/{cvId}")
    public ResponseEntity<Object> deleteCV(
            @PathVariable UUID cvId,
            Principal principal
    ) {
        UUID userId = getUserIdFromPrincipal(principal);
        cvServiceImpl.deleteCV(cvId, userId);
        return ResponseHandler.responseBuilder(
                "CV successfully deleted",
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
