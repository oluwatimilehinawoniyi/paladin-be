package com.paladin.mappers;

import com.paladin.dto.JobApplicationDTO;
import com.paladin.jobApplication.JobApplication;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface JobApplicationMapper {
    JobApplicationDTO toDTO(JobApplication jobApplication);

    JobApplication toEntity(JobApplicationDTO dto);
}

