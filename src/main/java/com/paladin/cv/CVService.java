package com.paladin.cv;

import com.paladin.dto.CVDTO;
import com.paladin.exceptions.CVNotFoundException;
import com.paladin.exceptions.InvalidFileException;
import com.paladin.mappers.CVMapper;
import com.paladin.profile.Profile;
import com.paladin.profile.ProfileRepository;
import com.paladin.s3_CV_Storage.S3CVStorageService;
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

    public CVDTO uploadCV(MultipartFile file, UUID profileId) {
        validateFile(file);

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(
                        () -> new RuntimeException("Profile not found"));

        if (profile.getCv() != null) {
            deleteCV(profile.getCv().getId());
        }

        String s3Url = s3CVStorageService.uploadFile(file, generateS3Key(
                Objects.requireNonNull(file.getOriginalFilename()),
                profileId
        ));

        CV cv = CV.builder()
                .fileName(file.getOriginalFilename())
                .url(s3Url)
                .uploadedAt(LocalDateTime.now())
                .size(file.getSize())
                .contentType(file.getContentType())
                .build();

        cvRepository.save(cv);

        profile.setCv(cv);
        profileRepository.save(profile);

        return cVMapper.toDTO(cv);
    }

    public CVDTO getCVById(UUID cvId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new CVNotFoundException("CV not " +
                        "found"));
        return cVMapper.toDTO(cv);
    }

    public CVDTO getCVbyProfileId(UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(
                        () -> new RuntimeException("Profile not found"));

        if (profile.getCv() == null) {
            throw new CVNotFoundException(
                    "No CV found for profile: " + profile.getTitle());
        }

        return cVMapper.toDTO(profile.getCv());
    }

    public CVDTO updateCV(UUID cvId, MultipartFile file) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new CVNotFoundException("CV not " +
                        "found for id: " + cvId));
        validateFile(file);

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

    public byte[] downloadCV(UUID cvId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new CVNotFoundException("CV not " +
                        "found"));

        String key = extractKeyFromUrl(cv.getUrl());
        return s3CVStorageService.downloadFile(key);
    }

    public void deleteCV(UUID cvId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new CVNotFoundException("CV not " +
                        "found"));

//      detach cv from profiles
        List<Profile> profiles = profileRepository.findAllByCvId(cvId);
        for (Profile profile : profiles) {
            profile.setCv(null);
        }

        String key = extractKeyFromUrl(cv.getUrl());

        s3CVStorageService.deleteFile(key);
        cvRepository.delete(cv);
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
