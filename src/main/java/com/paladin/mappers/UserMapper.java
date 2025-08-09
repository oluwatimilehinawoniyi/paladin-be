package com.paladin.mappers;

import com.paladin.dto.UserDTO;
import com.paladin.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User user);
}
