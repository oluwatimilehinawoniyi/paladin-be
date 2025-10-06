package com.paladin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.frontend.url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Paladin API")
                        .version("2.0.0")
                        .description("""
                                ## AI-Powered Job Application Assistant API
                                
                                Paladin streamlines job applications by leveraging AI to analyze CVs, 
                                generate tailored cover letters, and manage job application workflows 
                                with automated email submission.
                                
                                ### Key Features:
                                - **JWT Authentication** with OAuth2 Google Sign-In
                                - **Profile Management** with multiple professional profiles
                                - **CV Storage** using AWS S3
                                - **AI Job Analysis** powered by Claude AI
                                - **Automated Email Sending** via Gmail API
                                - **Application Tracking** with status management
                                
                                ### Authentication:
                                This API uses JWT (JSON Web Tokens) for authentication. To authenticate:
                                1. Navigate to `/oauth2/authorization/google` to sign in with Google
                                2. After successful OAuth, you'll receive JWT tokens
                                3. Include the access token in the Authorization header for all API calls:
                                   `Authorization: Bearer <your_access_token>`
                                4. When the access token expires, use the refresh token at `/api/auth/refresh`
                                
                                ### Token Management:
                                - Access tokens expire after 30 minutes
                                - Refresh tokens expire after 30 days
                                - Use `/api/auth/refresh` to get new tokens
                                
                                ### Rate Limits:
                                - AI Analysis: Limited by Anthropic API quotas
                                - Email Sending: Limited by Gmail API quotas
                                - File Upload: Max 5MB per CV
                                
                                ### Error Handling:
                                All endpoints return standardized error responses with:
                                - `message`: Human-readable error description
                                - `httpStatus`: HTTP status code
                                - `timestamp`: Error occurrence time (if applicable)
                                """)
                        .contact(new Contact()
                                .name("Paladin Support")
                                .email("info.paladinhq@gmail.com")
                                .url("https://github.com/oluwatimilehinawoniyi/paladin-be"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description("Development Server"),
                        new Server()
                                .url("https://paladin-be-8eieva.fly.dev")
                                .description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token in the format: Bearer <token>")));
    }
}