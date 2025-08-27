package com.paladin.mappers;

import com.paladin.dto.JobApplicationDTO;
import com.paladin.dto.NewJobApplicationDTO;
import com.paladin.jobApplication.JobApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobApplicationMapper {

    @Mapping(target = "profile", source = "profile.title")
    JobApplicationDTO toDTO(JobApplication jobApplication);

    JobApplication toEntity(NewJobApplicationDTO dto);
}

