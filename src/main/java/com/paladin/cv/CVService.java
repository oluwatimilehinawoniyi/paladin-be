package com.paladin.cv;

import com.paladin.dto.CVDTO;
import com.paladin.exceptions.CVNotFoundException;
import com.paladin.exceptions.InvalidFileException;
import com.paladin.exceptions.ProfileNotFoundException;
import com.paladin.exceptions.UnauthorizedAccessException;
import com.paladin.mappers.CVMapper;
import com.paladin.profile.Profile;
import com.paladin.profile.ProfileRepository;
import com.paladin.s3_CV_Storage.S3CVStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static com.paladin.utils.FileUtils.extractKeyFromUrl;

@Service
@RequiredArgsConstructor
@Slf4j
public class CVService {

    private final CVRepository cvRepository;
    private final ProfileRepository profileRepository;
    private final CVMapper cVMapper;
    private final S3CVStorageService s3CVStorageService;

    // 5MB limit for file/cv upload
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument" +
                    ".wordprocessingml.document"
    );

    @Transactional
    public CVDTO uploadCV(MultipartFile file,
                          UUID profileId,
                          UUID userId) {
        validateFile(file);

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(
                        () -> new RuntimeException("Profile not found"));

        if (!profile.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not " +
                    "authorized to upload CV to this profile.");
        }

        CV oldCV = profile.getCv();
        if (oldCV != null) {
            String oldKey = extractKeyFromUrl(oldCV.getUrl());
            s3CVStorageService.deleteFile(oldKey);
            profile.setCv(null);
            cvRepository.delete(oldCV);
            log.info("Deleted old CV (ID: {}) for profile (ID: {})",
                    oldCV.getId(), profile.getId());
        }

        String fileName = file.getOriginalFilename();
        String s3Key = generateS3Key(Objects.requireNonNull(fileName),
                UUID.randomUUID());
        String s3CVUrl = s3CVStorageService.uploadFile(file, s3Key);

        CV newCv = CV.builder()
                .fileName(file.getOriginalFilename())
                .url(s3CVUrl)
                .uploadedAt(LocalDateTime.now())
                .size(file.getSize())
                .contentType(file.getContentType())
                .build();

        CV savedCv = cvRepository.save(newCv);
        profile.setCv(savedCv);
        profileRepository.save(profile);

        log.info("Uploaded new CV (ID: {}) for profile (ID: {})",
                savedCv.getId(), profileId);
        return cVMapper.toDTO(savedCv);
    }

    public CVDTO getCVById(UUID cvId, UUID userId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new CVNotFoundException("CV not " +
                        "found"));
        Profile profile = profileRepository.findByCvId(cvId)
                .orElseThrow(() -> new CVNotFoundException(
                        "CV is not attached to any profile"));

        if (!profile.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException(
                    "You are not authorized to view this CV.");
        }
        return cVMapper.toDTO(cv);
    }

    public CVDTO getCVbyProfileId(UUID profileId, UUID userId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(
                        () -> new ProfileNotFoundException(
                                "Profile not found"));
        if (!profile.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException(
                    "You are not authorized to view this CV.");
        }

        if (profile.getCv() == null) {
            throw new CVNotFoundException(
                    "No CV found for profile: " + profile.getTitle());
        }

        return cVMapper.toDTO(profile.getCv());
    }

    public CVDTO updateCV(UUID cvId, MultipartFile file, UUID userId) {
        validateFile(file);
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new CVNotFoundException("CV not " +
                        "found for id: " + cvId));

        Profile profile = profileRepository.findByCvId(cvId)
                .orElseThrow(() -> new CVNotFoundException(
                        "CV is not associated with any profile for update."));

        if (!profile.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException(
                    "You are not authorized to update this CV.");
        }

        String oldKey = extractKeyFromUrl(cv.getUrl());
        s3CVStorageService.deleteFile(oldKey);

        String newKey = generateS3Key(
                Objects.requireNonNull(file.getOriginalFilename()), cvId);
        String newUrl = s3CVStorageService.uploadFile(file, newKey);

        // update CV entity
        cv.setFileName(file.getOriginalFilename());
        cv.setUrl(newUrl);
        cv.setSize(file.getSize());
        cv.setContentType(file.getContentType());
        cv.setUploadedAt(LocalDateTime.now());

        CV updatedCV = cvRepository.save(cv);
        return cVMapper.toDTO(updatedCV);
    }

    public byte[] downloadCV(UUID cvId, UUID userId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new CVNotFoundException("CV not " +
                        "found"));
        Profile profile = profileRepository.findByCvId(cvId)
                .orElseThrow(() -> new CVNotFoundException("CV is not " +
                        "associated with any profile."));


        if (!profile.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException(
                    "You are not authorized to view this CV.");
        }

        String key = extractKeyFromUrl(cv.getUrl());
        return s3CVStorageService.downloadFile(key);
    }

    public void deleteCV(UUID cvId, UUID userId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new CVNotFoundException("CV not " +
                        "found"));

//      detach cv from profiles
        List<Profile> profiles = profileRepository.findAllByCvId(cvId)
                .orElseThrow(() -> new CVNotFoundException("CV is not " +
                        "associated with any profile."));
        boolean authorized = false;
        for (Profile profile : profiles) {
            if (profile.getUser().getId().equals(userId)) {
                authorized = true;
                profile.setCv(null);
                profileRepository.save(profile);
                log.info("Detached CV (ID: {}) from profile (ID: {})",
                        cvId, profile.getId());
            }
        }

        if (!authorized) {
            throw new UnauthorizedAccessException(
                    "You are not authorized to delete this CV.");
        }

        String key = extractKeyFromUrl(cv.getUrl());

        s3CVStorageService.deleteFile(key);
        log.info("Deleted CV (ID: {}) from S3", cvId);
        cvRepository.delete(cv);
        log.info("Deleted CV (ID: {}) from database", cvId);
    }

//     Utils

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (!ALLOWED_FILE_TYPES.contains(file.getContentType())) {
            throw new InvalidFileException("File type not supported. " +
                    "Only PDF and Word documents are supported");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeds maximum " +
                    "limit of 5MB");
        }
    }

    private String generateS3Key(
            String originalFileName,
            UUID id
    ) {
        String extension = originalFileName.substring(
                originalFileName.lastIndexOf('.')
        );
        return String.format("cvs/%s_%s%s", id, System.currentTimeMillis(),
                extension);
    }

}
