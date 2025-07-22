package com.paladin.user.service;

import com.paladin.dto.UserDTO;
import com.paladin.mappers.UserMapper;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDTO getUserById(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        return userMapper.toDTO(user);
    }

    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
        return userMapper.toDTO(user);
    }
}
