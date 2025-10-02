package com.paladin.common.mappers;

import com.paladin.common.dto.ProfileCreateRequestDTO;
import com.paladin.common.dto.ProfileResponseDTO;
import com.paladin.common.dto.ProfileSummaryDTO;
import com.paladin.common.dto.ProfileUpdateRequestDTO;
import com.paladin.profile.Profile;
import com.paladin.user.User;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring", uses = {CVMapper.class})
public interface ProfileMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "cv", target = "cv")
    ProfileResponseDTO toResponseDTO(Profile profile);

    @Mapping(target = "id", ignore = true) // DB generates ID
    @Mapping(target = "user", ignore = true) // Service sets user
    @Mapping(target = "createdAt", ignore = true)
    Profile toEntity(ProfileCreateRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfileFromDto(ProfileUpdateRequestDTO dto,
                              @MappingTarget Profile profile);

    @Mapping(source = "cv", target = "cv")
    ProfileSummaryDTO toSummaryDTO(Profile profile);

    default User map(UUID userId) {
        if (userId == null) {
            return null;
        }
        User user = new User();
        user.setId(userId);
        return user;
    }
}

