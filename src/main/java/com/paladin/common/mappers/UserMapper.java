package com.paladin.common.mappers;

import com.paladin.common.dto.UserDTO;
import com.paladin.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User user);
}
