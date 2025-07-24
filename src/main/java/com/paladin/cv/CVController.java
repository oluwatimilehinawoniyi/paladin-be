package com.paladin.cv;

import com.paladin.dto.CVDTO;
import com.paladin.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cv")
@Tag(name = "CV Management",
        description = "Endpoints for managing CV uploads")
public class CVController {

    private final CVService cvService;
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
    public ResponseEntity<CVDTO> uploadCV(
            @RequestParam("file") MultipartFile file,
            @RequestParam("profileId") UUID profileId,
            Principal principal
    ) {
        UUID userId =
                userService.getUserByEmail(principal.getName()).getId();
        CVDTO cvdto = cvService.uploadCV(file, profileId, userId);
        return ResponseEntity.ok(cvdto);
    }

    @GetMapping("/{cvId}")
    public ResponseEntity<CVDTO> getCVById(
            @PathVariable UUID cvId,
            Principal principal
    ) {
        UUID userId =
                userService.getUserByEmail(principal.getName()).getId();
        CVDTO cvdto = cvService.getCVById(cvId, userId);
        return ResponseEntity.ok(cvdto);
    }

    @GetMapping("/profile/{profileId}")
    public ResponseEntity<CVDTO> getCVByProfileId(
            @PathVariable UUID profileId, Principal principal) {
        UUID userId =
                userService.getUserByEmail(principal.getName()).getId();
        CVDTO cvdto = cvService.getCVbyProfileId(profileId, userId);
        return ResponseEntity.ok(cvdto);
    }

    @GetMapping("/{cvId}/download")
    public void downloadCV(@PathVariable UUID cvId,
                           HttpServletResponse response,
                           Principal principal)
            throws IOException {
        UUID userId =
                userService.getUserByEmail(principal.getName()).getId();
        byte[] cvData = cvService.downloadCV(cvId, userId);
        CVDTO cvDTO = cvService.getCVById(cvId, userId);

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
    public ResponseEntity<CVDTO> updateCV(
            @PathVariable UUID cvId,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        UUID userId =
                userService.getUserByEmail(principal.getName()).getId();
        CVDTO updatedCV = cvService.updateCV(cvId, file, userId);
        return ResponseEntity.ok(updatedCV);
    }

    @DeleteMapping("/{cvId}")
    public ResponseEntity<Void> deleteCV(
            @PathVariable UUID cvId,
            Principal principal
    ) {
        UUID userId =
                userService.getUserByEmail(principal.getName()).getId();
        cvService.deleteCV(cvId, userId);
        return ResponseEntity.noContent().build();
    }
}
