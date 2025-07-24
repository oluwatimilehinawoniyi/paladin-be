package com.paladin.user.service;

import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        log.info("Attempting to load user by email: {}",
                email); // Use logger
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("User not found: {}", email);
            throw new UsernameNotFoundException("User not found");
        }

        log.info("User found: {} with ID: {}", user.getEmail(),
                user.getId());

        if (user.getPassword() == null) {
            throw new BadCredentialsException(
                    "Password not set. Use OAuth login.");
        }

        if (!user.isEmailVerified()) {
            throw new BadCredentialsException(
                    "Email not verified. Please check your inbox for a verification link.");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities("USER")
                .build();
    }
}
