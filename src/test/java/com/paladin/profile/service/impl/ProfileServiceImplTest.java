package com.paladin.profile.service.impl;

import com.paladin.common.dto.*;
import com.paladin.common.exceptions.NotFoundException;
import com.paladin.common.mappers.ProfileMapper;
import com.paladin.cv.CV;
import com.paladin.cv.repository.CVRepository;
import com.paladin.cv.service.impl.CVServiceImpl;
import com.paladin.profile.Profile;
import com.paladin.profile.repository.ProfileRepository;
import com.paladin.s3_CV_Storage.S3CVStorageService;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProfileMapper profileMapper;

    @Mock
    private CVRepository cvRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CVServiceImpl cvService;

    @Mock
    private S3CVStorageService s3CVStorageService;

    @InjectMocks
    private ProfileServiceImpl profileService;


    private UUID userId;
    private UUID profileId;
    private User testUser;
    private Profile testProfile;
    private CV cv = new CV();

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profileId = UUID.randomUUID();

        testUser = createTestUser();
        testProfile = createTestProfile();

        cv.setUrl("https://bucket.s3.amazonaws.com/cv-123.pdf");
        testProfile.setCv(cv);
    }

    @Test
    void shouldCreateProfileWithCV() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);

        ProfileCreateRequestDTO request = new ProfileCreateRequestDTO();
        request.setTitle("Backend Dev");
        request.setSummary("Test summary");
        request.setSkills(List.of("Java", "Spring"));
        request.setFile(mockFile);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));

        Profile newProfile = new Profile();
        newProfile.setTitle("Backend Dev");
        newProfile.setUser(testUser);

        when(profileMapper.toEntity(any(ProfileCreateRequestDTO.class)))
                .thenReturn(newProfile);

        Profile savedProfile = new Profile();
        savedProfile.setId(profileId);
        savedProfile.setTitle("Backend Dev");
        savedProfile.setUser(testUser);

        when(profileRepository.save(any(Profile.class)))
                .thenReturn(savedProfile);

        CVDTO cvDTO = new CVDTO();
        cvDTO.setId(UUID.randomUUID());
        when(cvService.uploadCV(any(MultipartFile.class), any(UUID.class), eq(userId)))
                .thenReturn(cvDTO);

        CV cv = new CV();
        cv.setId(cvDTO.getId());
        when(cvService.getCVByIdAsEntity(any(UUID.class)))
                .thenReturn(cv);

        ProfileResponseDTO responseDTO = ProfileResponseDTO.builder()
                .id(profileId)
                .title("Backend Dev")
                .summary("Test summary")
                .skills(List.of("Java", "Spring"))
                .userId(userId)
                .build();

        when(profileMapper.toResponseDTO(any(Profile.class)))
                .thenReturn(responseDTO);

        ProfileResponseDTO result = profileService.createProfileWithCV(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Backend Dev");

        verify(userRepository, times(1)).findById(userId);
        verify(cvService, times(1)).uploadCV(any(), any(), eq(userId));
        verify(profileRepository, times(2)).save(any(Profile.class));
    }

    @Test
    void shouldCreateProfileWithoutCV() {
        ProfileCreateRequestDTO request = new ProfileCreateRequestDTO();
        request.setTitle("Backend Dev");
        request.setSummary("Test summary");
        request.setSkills(List.of("Java", "Spring"));
        request.setFile(null);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));

        Profile newProfile = new Profile();
        newProfile.setTitle("Backend Dev");
        newProfile.setUser(testUser);

        when(profileMapper.toEntity(any(ProfileCreateRequestDTO.class)))
                .thenReturn(newProfile);

        Profile savedProfile = new Profile();
        savedProfile.setId(profileId);
        savedProfile.setTitle("Backend Dev");
        savedProfile.setUser(testUser);

        when(profileRepository.save(any(Profile.class)))
                .thenReturn(savedProfile);

        ProfileResponseDTO responseDTO = ProfileResponseDTO.builder()
                .id(profileId)
                .title("Backend Dev")
                .summary("Test summary")
                .skills(List.of("Java", "Spring"))
                .userId(userId)
                .build();

        when(profileMapper.toResponseDTO(any(Profile.class)))
                .thenReturn(responseDTO);

        ProfileResponseDTO result = profileService.createProfileWithCV(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Backend Dev");

        verify(userRepository, times(1)).findById(userId);
        verify(cvService, never()).uploadCV(any(), any(), any());
        verify(profileRepository, times(1)).save(any(Profile.class));
    }

    @Test
    void shouldGetProfilesByUserIdSuccessfully() {
        Profile profile1 = new Profile();
        profile1.setId(UUID.randomUUID());
        profile1.setTitle("Backend Dev");

        Profile profile2 = new Profile();
        profile2.setId(UUID.randomUUID());
        profile2.setTitle("Frontend Dev");

        List<Profile> profiles = List.of(profile1, profile2);

        ProfileSummaryDTO dto1 = new ProfileSummaryDTO();
        dto1.setId((profile1.getId()));
        dto1.setTitle(profile1.getTitle());

        ProfileSummaryDTO dto2 = new ProfileSummaryDTO();
        dto2.setId((profile2.getId()));
        dto2.setTitle(profile2.getTitle());

        when(profileMapper.toSummaryDTO(profile1)).thenReturn(dto1);
        when(profileMapper.toSummaryDTO(profile2)).thenReturn(dto2);

        when(profileRepository.findByUserId(userId))
                .thenReturn(Optional.of(profiles));

        var result = profileService.getProfilesByUserId(userId);

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.getFirst().getTitle()).isEqualTo(dto1.getTitle());
        assertThat(result.getFirst().getId()).isEqualTo(dto1.getId());

        assertThat(result.get(1).getTitle()).isEqualTo(dto2.getTitle());
        assertThat(result.get(1).getId()).isEqualTo(dto2.getId());

        verify(profileRepository, times(1)).findByUserId(userId);
        verify(profileMapper, times(2)).toSummaryDTO(any(Profile.class));
    }

    @Test
    void shouldGetProfileByIdSuccessfully() {
        when(profileRepository.findById(profileId))
                .thenReturn(Optional.of(testProfile));

        ProfileResponseDTO profileResponseDTO = ProfileResponseDTO.builder()
                .id(profileId)
                .title(testProfile.getTitle())
                .summary(testProfile.getSummary())
                .skills(testProfile.getSkills())
                .userId(userId)
                .build();

        when(profileMapper.toResponseDTO(any(Profile.class)))
                .thenReturn(profileResponseDTO);

        var result = profileService.getProfileById(profileId, userId);

        assertNotNull(result);
        assertThat(result.getId()).isEqualTo(profileResponseDTO.getId());
        assertThat(result.getTitle()).isEqualTo(profileResponseDTO.getTitle());

        verify(profileRepository, times(1)).findById(profileId);
        verify(profileMapper, times(1)).toResponseDTO(any(Profile.class));
    }

    @Test
    void ShouldThrowExceptionWhenProfileNotFoundForUser() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                profileService.getProfilesByUserId(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Profile not found");

        verify(profileMapper, never()).toSummaryDTO(any());
    }

    @Test
    void shouldUpdateProfileSuccessfully() {
        var update = new ProfileUpdateRequestDTO();
        update.setTitle("New Title");
        update.setSummary("This is a test stuff");
        update.setSkills(List.of("Java", "Kotlin", "Cobol"));

        when(profileRepository
                .findById(profileId))
                .thenReturn(Optional.of(testProfile));

        doNothing()
                .when(profileMapper)
                .updateProfileFromDto(
                        any(ProfileUpdateRequestDTO.class),
                        any(Profile.class));

        when(profileRepository.save(any(Profile.class)))
                .thenReturn(testProfile);

        ProfileResponseDTO responseDTO = ProfileResponseDTO.builder()
                .id(profileId)
                .title("New Title")
                .summary("This is a test stuff")
                .skills(List.of("Java", "Kotlin", "Cobol"))
                .userId(userId)
                .build();

        when(profileMapper.toResponseDTO(any(Profile.class)))
                .thenReturn(responseDTO);

        var result = profileService.
                updateProfile(userId, profileId, update);

        assertThat(result.getTitle()).isEqualTo(update.getTitle());
        assertThat(result.getSummary()).isEqualTo(update.getSummary());
        assertThat(result.getSkills()).isEqualTo(update.getSkills());

        verify(profileMapper, times(1)).toResponseDTO(any(Profile.class));
        verify(profileMapper, times(1)).updateProfileFromDto(any(ProfileUpdateRequestDTO.class), any(Profile.class));
        verify(profileRepository, times(1)).save(any(Profile.class));
        verify(profileRepository, times(1)).findById(profileId);
    }

    @Test
    void shouldDeleteProfileSuccessfully() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(testProfile));

        doNothing()
                .when(profileRepository).delete(testProfile);

        profileService.deleteProfile(profileId, userId);

        verify(profileRepository, times(1)).findById(profileId);
        verify(profileRepository, times(1)).delete(testProfile);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentProfile() {
        when(profileRepository.findById(profileId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                profileService.deleteProfile(profileId, userId)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Profile not found");

        verify(profileRepository, never()).delete(any());
    }

    private User createTestUser() {
        User user = new User();
        user.setId(userId);
        user.setEmail("test@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        return user;
    }

    private Profile createTestProfile() {
        Profile profile = new Profile();
        profile.setId(profileId);
        profile.setTitle("Software Engineer");
        profile.setSummary("Experienced developer");
        profile.setSkills(List.of("Java", "Spring"));
        profile.setUser(testUser);
        profile.setCv(cv);
        return profile;
    }
}