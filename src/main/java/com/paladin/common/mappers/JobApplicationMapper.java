package com.paladin.common.mappers;

import com.paladin.common.dto.JobApplicationDTO;
import com.paladin.common.dto.NewJobApplicationDTO;
import com.paladin.jobApplication.JobApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobApplicationMapper {

    @Mapping(target = "profile", source = "profile.title")
    JobApplicationDTO toDTO(JobApplication jobApplication);

    JobApplication toEntity(NewJobApplicationDTO dto);
}

