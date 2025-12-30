package com.paladin.cv.service.impl;

import com.paladin.common.dto.CVDTO;
import com.paladin.common.exceptions.InvalidFileException;
import com.paladin.common.mappers.CVMapper;
import com.paladin.cv.CV;
import com.paladin.cv.repository.CVRepository;
import com.paladin.profile.Profile;
import com.paladin.profile.repository.ProfileRepository;
import com.paladin.s3_CV_Storage.S3CVStorageService;
import com.paladin.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CVServiceImplTest {

    @Mock
    private CVRepository cvRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private S3CVStorageService s3CVStorageService;

    @Mock
    private CVMapper cvMapper;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private CVServiceImpl cvService;

    private UUID userId;
    private UUID profileId;
    private UUID cvId;
    private Profile testProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profileId = UUID.randomUUID();
        cvId = UUID.randomUUID();

        testProfile = createTestProfile();
    }

    @Test
    void shouldUploadCVSuccessfully() throws IOException {
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(1024L);

        when(profileRepository.findById(profileId))
                .thenReturn(Optional.of(testProfile));

        String fakeS3Url = "https://bucket.s3.amazonaws.com/cv-123.pdf";
        when(s3CVStorageService.uploadFile(any(MultipartFile.class), anyString()))
                .thenReturn(fakeS3Url);

        CV savedCV = new CV();
        savedCV.setId(cvId);
        savedCV.setFileName("resume.pdf");
        savedCV.setUrl(fakeS3Url);
        savedCV.setSize(1024L);

        when(cvRepository.save(any(CV.class)))
                .thenReturn(savedCV);

        CVDTO cvDTO = new CVDTO();
        cvDTO.setId(cvId);
        cvDTO.setFileName("resume.pdf");
        cvDTO.setUrl(fakeS3Url);

        when(cvMapper.toDTO(any(CV.class)))
                .thenReturn(cvDTO);

        CVDTO result = cvService.uploadCV(mockFile, profileId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("resume.pdf");
        assertThat(result.getUrl()).isEqualTo(fakeS3Url);

        verify(s3CVStorageService, times(1))
                .uploadFile(any(MultipartFile.class), anyString());

        verify(cvRepository, times(1)).save(any(CV.class));
    }

    @Test
    void shouldDownloadCVSuccessfully() {
        CV cv = new CV();
        cv.setId(cvId);
        cv.setUrl("https://bucket.s3.amazonaws.com/cv-123.pdf");

        when(cvRepository.findById(cvId))
                .thenReturn(Optional.of(cv));

        when(profileRepository.findByCvId(cvId))
                .thenReturn(Optional.of(testProfile));

        byte[] fakeBytes = "PDF content".getBytes();
        when(s3CVStorageService.downloadFile(anyString()))
                .thenReturn(fakeBytes);

        byte[] result = cvService.downloadCV(cvId, userId);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(fakeBytes);

        verify(s3CVStorageService, times(1))
                .downloadFile(anyString());
    }


    @Test
    void shouldThrowExceptionWhenFileTooBig() {
        when(mockFile.getSize()).thenReturn(10 * 1024 * 1024L);
        when(mockFile.getContentType()).thenReturn("application/pdf");

        assertThatThrownBy(
                () -> cvService.uploadCV(mockFile, profileId, userId)
        )
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("File size exceeds");


        verify(s3CVStorageService, never()).uploadFile(any(), any());
    }


    private Profile createTestProfile() {
        Profile profile = new Profile();
        profile.setId(profileId);

        User user = new User();
        user.setId(userId);
        profile.setUser(user);

        return profile;
    }
}