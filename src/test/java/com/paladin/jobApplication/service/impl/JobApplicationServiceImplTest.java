package com.paladin.jobApplication.service.impl;

import com.paladin.common.dto.*;
import com.paladin.common.enums.ApplicationStatus;
import com.paladin.common.enums.AuthProvider;
import com.paladin.common.exceptions.CannotSendMailException;
import com.paladin.common.exceptions.ProfileNotFoundException;
import com.paladin.common.mappers.JobApplicationMapper;
import com.paladin.cv.CV;
import com.paladin.cv.service.impl.CVServiceImpl;
import com.paladin.jobApplication.JobApplication;
import com.paladin.jobApplication.repository.JobApplicationRepository;
import com.paladin.profile.Profile;
import com.paladin.profile.repository.ProfileRepository;
import com.paladin.user.User;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplTest {


    @Mock
    private CVServiceImpl cvService;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private JobApplicationMapper jobApplicationMapper;

    @Mock
    private EmailProviderService emailProviderService;

    @InjectMocks
    private JobApplicationServiceImpl jobApplicationService;

    private UUID userId;
    private UUID profileId;
    private User testUserEntity;
    private CV testCVEntity;
    private Profile testProfileEntity;
    private NewJobApplicationDTO newJobAppRequest;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profileId = UUID.randomUUID();

        testCVEntity = createTestCVEntity();
        testUserEntity = createTestUserEntity();
        testProfileEntity = createTestProfileEntity();
        newJobAppRequest = createTestJobApplicationRequest();
        applicationId = UUID.randomUUID();

        User userEntity = new User();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setFirstName("John");
        userEntity.setLastName("Doe");
    }

    @Test
    void shouldCreateAndSendJobApplicationSuccessfully() throws MessagingException, IOException {
        when(profileRepository.findById(profileId))
                .thenReturn(Optional.of(testProfileEntity));

        when(emailProviderService.canUserSendEmails(any(User.class)))
                .thenReturn(true);

        byte[] mockCVData = "Mock PDF Content".getBytes();
        when(cvService.downloadCV(any(UUID.class), eq(userId)))
                .thenReturn(mockCVData);

        JobApplication jobApplicationEntity = new JobApplication();
        jobApplicationEntity.setCompany("Tech Corp");
        jobApplicationEntity.setJobTitle("Senior Developer");
        jobApplicationEntity.setJobEmail("hr@techcorp.com");

        when(jobApplicationMapper.toEntity(any(NewJobApplicationDTO.class)))
                .thenReturn(jobApplicationEntity);

        JobApplication savedJobApp = new JobApplication();
        savedJobApp.setId(UUID.randomUUID());
        savedJobApp.setCompany("Tech Corp");
        savedJobApp.setJobTitle("Senior Developer");
        savedJobApp.setStatus(ApplicationStatus.SENT);
        savedJobApp.setSentAt(LocalDateTime.now());

        when(jobApplicationRepository.save(any(JobApplication.class)))
                .thenReturn(savedJobApp);

        JobApplicationDTO resultDTO = new JobApplicationDTO();
        resultDTO.setId(savedJobApp.getId());
        resultDTO.setCompany("Tech Corp");
        resultDTO.setJobTitle("Senior Developer");
        resultDTO.setStatus(ApplicationStatus.SENT);
        resultDTO.setProfile("Software Engineer");

        when(jobApplicationMapper.toDTO(any(JobApplication.class)))
                .thenReturn(resultDTO);

        doNothing().when(emailProviderService)
                .sendJobApplicationEmail(
                        any(User.class),
                        any(JobApplicationEmailRequest.class)
                );

        JobApplicationDTO result = jobApplicationService
                .createAndSendJobApplication(newJobAppRequest, userId);

        assertThat(result).isNotNull();
        assertThat(result.getCompany()).isEqualTo("Tech Corp");
        assertThat(result.getJobTitle()).isEqualTo("Senior Developer");
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.SENT);

        verify(profileRepository, times(1)).findById(profileId);
        verify(emailProviderService, times(1)).canUserSendEmails(any(User.class));
        verify(cvService, times(1)).downloadCV(any(UUID.class), eq(userId));
        verify(jobApplicationMapper, times(1)).toEntity(any(NewJobApplicationDTO.class));
        verify(jobApplicationRepository, times(1)).save(any(JobApplication.class));
        verify(jobApplicationMapper, times(1)).toDTO(any(JobApplication.class));
        verify(emailProviderService, times(1)).sendJobApplicationEmail(any(User.class), any(JobApplicationEmailRequest.class));
    }

    @Test
    void shouldGetJobApplicationsByUserId() {
        JobApplication app1 = new JobApplication();
        app1.setId(UUID.randomUUID());
        app1.setCompany("Company A");
        app1.setJobTitle("Backend Dev");
        app1.setStatus(ApplicationStatus.SENT);

        JobApplication app2 = new JobApplication();
        app2.setId(UUID.randomUUID());
        app2.setCompany("Company B");
        app2.setJobTitle("Frontend Dev");
        app2.setStatus(ApplicationStatus.INTERVIEW);

        List<JobApplication> mockApplications = List.of(app1, app2);

        when(jobApplicationRepository.findByProfileUserId(userId))
                .thenReturn(mockApplications);

        JobApplicationDTO dto1 = new JobApplicationDTO();
        dto1.setId(app1.getId());
        dto1.setCompany("Company A");
        dto1.setJobTitle("Backend Dev");
        dto1.setStatus(ApplicationStatus.SENT);

        JobApplicationDTO dto2 = new JobApplicationDTO();
        dto2.setId(app2.getId());
        dto2.setCompany("Company B");
        dto2.setJobTitle("Frontend Dev");
        dto2.setStatus(ApplicationStatus.INTERVIEW);

        when(jobApplicationMapper.toDTO(app1)).thenReturn(dto1);
        when(jobApplicationMapper.toDTO(app2)).thenReturn(dto2);

        List<JobApplicationDTO> result = jobApplicationService.getJobApplicationsByUserId(userId);

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getCompany()).isEqualTo("Company A");
        assertThat(result.get(1).getCompany()).isEqualTo("Company B");

        verify(jobApplicationRepository, times(1))
                .findByProfileUserId(userId);

        verify(jobApplicationMapper, times(2))
                .toDTO(any(JobApplication.class));
    }

    @Test
    void shouldUpdateJobApplicationStatus() {
        JobApplication existingApp = new JobApplication();
        existingApp.setId(applicationId);
        existingApp.setStatus(ApplicationStatus.SENT);
        existingApp.setProfile(testProfileEntity);

        when(jobApplicationRepository.findById(applicationId))
                .thenReturn(Optional.of(existingApp));

        when(jobApplicationRepository.save(existingApp))
                .thenReturn(existingApp);

        JobApplicationDTO resultDTO = new JobApplicationDTO();
        resultDTO.setId(applicationId);
        resultDTO.setStatus(ApplicationStatus.ACCEPTED);

        when(jobApplicationMapper.toDTO(existingApp))
                .thenReturn(resultDTO);

        JobApplicationDTO result = jobApplicationService
                .updateJobApplicationStatus(applicationId, ApplicationStatus.ACCEPTED, userId);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);

        assertThat(existingApp.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);

        verify(jobApplicationRepository).findById(applicationId);
        verify(jobApplicationRepository).save(existingApp);
    }

    @Test
    void shouldThrowExceptionWhenProfileNotFound() throws MessagingException, IOException {
        when(profileRepository.findById(profileId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobApplicationService.createAndSendJobApplication(newJobAppRequest, userId))
                .isInstanceOf(ProfileNotFoundException.class)
                .hasMessageContaining("Profile not found");

        verify(emailProviderService, never()).sendJobApplicationEmail(any(), any());
    }

    @Test
    void shouldThrowExceptionWhenEmailSendingFails() throws MessagingException, IOException {
        when(profileRepository.findById(profileId))
                .thenReturn(Optional.of(testProfileEntity));

        when(emailProviderService.canUserSendEmails(any(User.class)))
                .thenReturn(true);

        byte[] mockCVData = "Mock PDF".getBytes();
        when(cvService.downloadCV(any(UUID.class), eq(userId)))
                .thenReturn(mockCVData);

        JobApplication entity = new JobApplication();
        entity.setCompany("Tech Corp");
        when(jobApplicationMapper.toEntity(any(NewJobApplicationDTO.class)))
                .thenReturn(entity);

        doThrow(new MessagingException("SMTP server down"))
                .when(emailProviderService)
                .sendJobApplicationEmail(any(User.class), any(JobApplicationEmailRequest.class));

        assertThatThrownBy(() -> jobApplicationService.createAndSendJobApplication(newJobAppRequest, userId))
                .isInstanceOf(CannotSendMailException.class)
                .hasMessageContaining("Failed to send job application email");

        verify(jobApplicationRepository, never()).save(any());
    }

    private User createTestUserEntity() {
        User user = new User();
        user.setId(userId);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        user.setAccessToken("mock-google-access-token");
        user.setRefreshToken("mock-google-refresh-token");
        user.setAccessTokenExpiry(LocalDateTime.now().plusHours(1));
        return user;
    }

    private Profile createTestProfileEntity() {
        Profile profile = new Profile();
        profile.setId(profileId);
        profile.setTitle("Software Engineer");
        profile.setSummary("Experienced dev");
        profile.setSkills(List.of("Java", "Spring"));
        profile.setUser(testUserEntity);
        profile.setCv(testCVEntity);
        return profile;
    }

    private CV createTestCVEntity() {
        CV cv = new CV();
        cv.setId(UUID.randomUUID());
        cv.setFileName("john_doe_resume.pdf");
        cv.setUrl("https://s3.amazonaws.com/bucket/cv123.pdf");
        cv.setContentType("application/pdf");
        cv.setSize(1024L);
        cv.setUploadedAt(LocalDateTime.now());
        // cv.setProfile(testProfileEntity);  // Don't set this yet - circular reference
        return cv;
    }

    private NewJobApplicationDTO createTestJobApplicationRequest() {
        return NewJobApplicationDTO.builder()
                .profileId(profileId)
                .company("Tech Corp")
                .jobTitle("Senior Developer")
                .jobEmail("hr@techcorp.com")
                .subject("Application for Senior Developer Position")  // ✅ Add
                .bodyText("Dear Hiring Manager,\n\nI am writing to express my strong interest in the Senior Developer position at Tech Corp.\n\nBest regards,\nJohn Doe")
                .build();
    }
}