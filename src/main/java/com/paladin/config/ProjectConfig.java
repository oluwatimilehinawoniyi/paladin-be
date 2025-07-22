package com.paladin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

//@Configuration
public class ProjectConfig {

//    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        return http
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(
                        c -> c
                                .anyRequest().authenticated()
                )
                .build();
    }

//    @Bean
    public UserDetailsService userDetailsService() {
        var userDetailsService =
                new InMemoryUserDetailsManager();

        var user1 = User.withUsername("John")
                .password("12345")
                .roles("USER")
                .build();

        userDetailsService.createUser(user1);
        return userDetailsService;
    }

//    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
