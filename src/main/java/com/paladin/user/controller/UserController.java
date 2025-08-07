package com.paladin.user.controller;

import com.paladin.dto.UserResponseDTO; // Assuming you have this DTO now
import com.paladin.mappers.UserMapper; // You'll need UserMapper here
import com.paladin.response.ResponseHandler;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public ResponseEntity<Object> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = principal.getName();
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        // Map the User entity to UserResponseDTO
        UserResponseDTO userResponseDTO = userMapper.toResponseDTO(user);
        return ResponseHandler.responseBuilder(
                "User details successfully returned",
                HttpStatus.OK,
                userResponseDTO
        );
    }

}