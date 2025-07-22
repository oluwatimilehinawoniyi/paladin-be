package com.paladin.mappers;

import com.paladin.cv.CV;
import com.paladin.dto.CVSummaryDTO;
import com.paladin.dto.ProfileDTO;
import com.paladin.dto.ProfileSummaryDTO;
import com.paladin.profile.Profile;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {CVMapper.class})
public interface ProfileMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "cv", target = "cv")
    ProfileDTO toDTO(Profile profile);

    @Mapping(source = "userId", target = "user.id")
    Profile toEntity(ProfileDTO dto);

    @Mapping(source = "cv", target = "cv")
    ProfileSummaryDTO toSummaryDTO(Profile profile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfileFromDto(ProfileDTO dto, @MappingTarget Profile profile);

}

