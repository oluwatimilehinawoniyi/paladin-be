package com.paladin.user.controller;

import com.paladin.dto.UserDTO;
import com.paladin.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    @GetMapping
    public UserDTO getUser() {
        return null;
    }
}
