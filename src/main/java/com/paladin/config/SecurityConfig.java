package com.paladin.config;

import com.paladin.auth.OAuth2AuthenticationSuccessHandler;
import com.paladin.user.service.CustomOAuth2UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    @PostConstruct
    public void init() {
        log.error("🔧🔧🔧 SecurityConfig initialized with CustomOAuth2UserService: {}", customOAuth2UserService);
        log.error("🔧🔧🔧 OAuth2AuthenticationSuccessHandler: {}", oauth2SuccessHandler);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.error("🔧🔧🔧 Configuring SecurityFilterChain...");

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> {
                    log.error("🔧🔧🔧 Configuring OAuth2 login with custom user service: {}", customOAuth2UserService);
                    oauth2
                            .authorizationEndpoint(authorization -> authorization
                                    .baseUri("/oauth2/authorization")
                            )
                            .redirectionEndpoint(redirection -> redirection
                                    .baseUri("/oauth2/callback/*")
                            )
                            .userInfoEndpoint(userInfo -> {
                                log.error("🔧🔧🔧 Setting custom user service: {}", customOAuth2UserService);
                                OAuth2UserService<OAuth2UserRequest, OAuth2User> service = customOAuth2UserService;
                                userInfo.userService(service);
                            })
                            .successHandler(oauth2SuccessHandler);
//                            .defaultSuccessUrl("http://localhost:5173/auth/callback", true);
                })
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(
                                        "/oauth2/**",
                                        "/api/auth/me",
                                        "/api/auth/logout",
                                        "/api/debug/**",  // ADD THIS LINE!
                                        "/error"
                                )
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        log.error("🔧🔧🔧 SecurityFilterChain configured successfully");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}