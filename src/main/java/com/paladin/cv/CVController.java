package com.paladin.cv;

import com.paladin.dto.CVDTO;
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
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cv")
@Tag(name = "CV Management", description = "Endpoints for managing CV uploads")
public class CVController {

    private final CVService cvService;

    @Operation(
            summary = "Upload a CV",
            description = "Uploads a CV file to S3 and returns its key",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Upload successful"),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            }
    )
    @PostMapping("/upload")
    public ResponseEntity<CVDTO> uploadCV(
            @RequestParam("file") MultipartFile file,
            @RequestParam("profileId") UUID profileId
    ) {
        CVDTO cvdto = cvService.uploadCV(file, profileId);
        return ResponseEntity.ok(cvdto);
    }

    @GetMapping("/{cvId}")
    public ResponseEntity<CVDTO> getCVById(@PathVariable UUID cvId) {
        CVDTO cvdto = cvService.getCVById(cvId);
        return ResponseEntity.ok(cvdto);
    }

    @GetMapping("/profile/{profileId}")
    public ResponseEntity<CVDTO> getCVByProfileId(
            @PathVariable UUID profileId) {
        CVDTO cvdto = cvService.getCVbyProfileId(profileId);
        return ResponseEntity.ok(cvdto);
    }

    @GetMapping("/{cvId}/download")
    public void downloadCV(@PathVariable UUID cvId,
                           HttpServletResponse response)
            throws IOException {
        byte[] cvData = cvService.downloadCV(cvId);
        CVDTO cvDTO = cvService.getCVById(cvId);

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
            @RequestParam("file") MultipartFile file
    ) {
        CVDTO updatedCV = cvService.updateCV(cvId, file);
        return ResponseEntity.ok(updatedCV);
    }

    @DeleteMapping("/{cvId}")
    public ResponseEntity<Void> deleteCV(@PathVariable UUID cvId) {
        cvService.deleteCV(cvId);
        return ResponseEntity.noContent().build();
    }
}
