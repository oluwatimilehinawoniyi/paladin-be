package com.paladin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.google")
@Getter
@Setter
public class GoogleOAuthConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String[] scope;
}