package com.paladin.auth;

import com.paladin.auth.service.EmailService;
import com.paladin.dto.*;
import com.paladin.enums.AuthProvider;
import com.paladin.exceptions.UserNotFoundException;
import com.paladin.response.ResponseHandler;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RememberMeServices rememberMeServices;
    private final EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody @Valid UserRegisterRequestDTO dto,
            HttpServletRequest request) {

        Map<String, String> response = new HashMap<>();

        User existingUser =
                userRepository.findByEmail(dto.getEmail())
                        .orElse(null);

        if (existingUser != null) {
            if (existingUser.isEmailVerified()) {
                log.warn(
                        "Registration failed: email already registered and verified: {}",
                        dto.getEmail());
                return new ResponseEntity<>(createErrorResponse(
                        "User already exists!"), HttpStatus.BAD_REQUEST);
            } else {
                log.info("Attempting to re-register unverified user {}. " +
                                "Generating new activation code.",
                        dto.getEmail());

                String newActivationCode = UUID.randomUUID().toString();
                LocalDateTime newActivationExpiry =
                        LocalDateTime.now().plusMinutes(10);

                existingUser.setActivationCode(newActivationCode);
                existingUser.setActivationCodeExpiry(newActivationExpiry);
                userRepository.save(existingUser);

                String verificationLink =
                        ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/api/auth/verify-email")
                                .queryParam("token", newActivationCode)
                                .toUriString();

                String emailBody = String.format(
                        """
                                 Dear %s,
                                \s
                                 This is a confirmation email to verify your email address.                                                            \s
                                 Please click on the following link to \
                                 verify your email address:
                                 %s
                                \s
                                 This link \
                                 will expire in 10 minutes.
                                \s
                                 Thank you,\
                                \s
                                 Paladin Team""",
                        existingUser.getFirstName(),
                        verificationLink);


                emailService.sendVerificationEmail(existingUser.getEmail(),
                        existingUser.getFirstName(), emailBody);
                log.info(
                        "New verification email sent to unverified user: {}",
                        existingUser.getEmail());
                response.put("message",
                        "User already exists but email not verified. A new verification link has been sent to your email. Please check your inbox.");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        }


        // new user
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);
        user.setActivationCode(UUID.randomUUID().toString());
        user.setActivationCodeExpiry(LocalDateTime.now().plusMinutes(10));
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        // send email confirmation
        String verificationUrl =
                ServletUriComponentsBuilder
                        .fromCurrentContextPath()
                        .path("/api/auth/verify-email")
                        .queryParam("token", user.getActivationCode())
                        .toUriString();

        String emailSubject = "Paladin - Verify your email";
        String emailBody = String.format(
                """
                        Dear %s,
                        
                        Please click on the following link to \
                        verify your email address:
                        %s
                        
                        This link\
                        will expire in 10 minutes.
                        
                        Thank you,\
                        
                        Paladin Team""",
                user.getFirstName(),
                verificationUrl);

        try {
            emailService.sendVerificationEmail(user.getEmail(),
                    emailSubject,
                    emailBody);
            log.info("New user registered and verification email sent " +
                    "to: {}", dto.getEmail());
            response.put("message", "User registered successfully and " +
                    "Verification email sent to your mail!");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error sending verification email to {}: {}",
                    user.getFirstName(), e.getMessage());
            response.put("error",
                    "Registration Successful, but error sending " +
                            "verification email. Please contact support:" +
                            " " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestParam("token") String token
    ) {
        try {
            log.info("Verifying email for token: {}", token);
            User user =
                    userRepository.findByActivationCode(token).orElseThrow(
                            () -> new UserNotFoundException(
                                    "User not found for " +
                                            "token: " + token + ".")
                    );
            if (user == null) {
                log.warn(
                        "Email verification failed: Invalid or non-existent token provided: {}",
                        token);
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid verification code!"));
            }

            if (user.isEmailVerified()) {
                log.info(
                        "Email verification failed: Email already verified: {}",
                        user.getEmail());
                return new ResponseEntity<>(createErrorResponse("Email " +
                        "already verified!"), HttpStatus.CONFLICT);
            }

            if (user.getActivationCodeExpiry() == null || user.getActivationCodeExpiry()
                    .isBefore(LocalDateTime.now())) {
                log.warn(
                        "Email verification failed: Verification code has expired: {}",
                        token);
                return new ResponseEntity<>(
                        createErrorResponse("Verification " +
                                "code has expired! Please try registering again to receive a new link."),
                        HttpStatus.GONE);
            }
            user.setEmailVerified(true);
            user.setActivationCode(null);
            user.setActivationCodeExpiry(null);
            userRepository.save(user);
            log.info("Email successfully verified for user: {}",
                    user.getEmail());
            return ResponseEntity.ok(
                    Map.of("message", "Email verified successfully!"));
        } catch (UserNotFoundException e) {
            log.error("User not found during email verification for " +
                    "token {}: {}", token, e.getMessage());
            return new ResponseEntity<>(createErrorResponse("User not " +
                    "found for this verification code!"),
                    HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error(
                    "An unexpected error occurred during email verification for token {}: {}",
                    token, e.getMessage(), e);
            return new ResponseEntity<>(createErrorResponse(
                    "An unexpected error occurred during email verification."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(
            @RequestBody LoginDTO loginDto,
            HttpServletRequest request,
            HttpServletResponse httpServletResponse) {

        Map<String, Object> response = new HashMap<>();

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginDto.getEmail(),
                                    loginDto.getPassword()
                            )
                    );

            User user = userRepository.findByEmail(loginDto.getEmail())
                    .orElseThrow(() -> new UserNotFoundException(
                            "User with email not found."
                    ));
            if (user != null && !user.isEmailVerified()) {
                response.put("error", "Please verify your email address!" +
                        " Check your inbox for a verification link.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(response);
            }

            SecurityContext securityContext =
                    SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // Create a new session
            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext);

            if (loginDto.isRememberMe()) {
                try {
                    HttpServletRequestWrapper requestWrapper =
                            new HttpServletRequestWrapper(request) {
                                @Override
                                public String getParameter(String name) {
                                    if ("rememberMe".equals(name)) {
                                        return "true";
                                    }
                                    return super.getParameter(name);
                                }

                                @Override
                                public String[] getParameterValues(
                                        String name) {
                                    if ("rememberMe".equals(name)) {
                                        return new String[]{"true"};
                                    }
                                    return super.getParameterValues(name);
                                }

                                @Override
                                public Map<String, String[]> getParameterMap() {
                                    Map<String, String[]> params =
                                            new HashMap<>(
                                                    super.getParameterMap());
                                    params.put("rememberMe",
                                            new String[]{"true"});
                                    return params;
                                }
                            };
                    rememberMeServices.loginSuccess(requestWrapper,
                            httpServletResponse, authentication);
                    System.out.println(
                            "Remember-me cookie set successfully");
                } catch (Exception e) {
                    log.error("Failed to set remember-me cookie: {}",
                            e.getMessage());
                }
            }

            response.put("sessionId", session.getId());
            response.put("user", new UserResponseDTO() {{
                setId(Objects.requireNonNull(user).getId());
                setEmail(user.getEmail());
                setFirstName(user.getFirstName());
                setLastName(user.getLastName());
            }});
            return ResponseHandler.responseBuilder(
                    "Login successful!",
                    HttpStatus.OK,
                    response);

        } catch (BadCredentialsException e) {
            response.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        } catch (Exception e) {
            response.put("error",
                    "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        SecurityContextHolder.clearContext(); // Clear the security context
        HttpSession session =
                request.getSession(false); // Get session, don't create new
        if (session != null) {
            session.invalidate(); // Invalidate the session
            log.info("User logged out and session invalidated.");
        }

        // Clear remember-me cookie if it exists
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("REMEMBER_ME_COOKIE".equals(
                        cookie.getName())) { // Match your remember-me cookie name
                    cookie.setMaxAge(0); // Set max age to 0 to delete
                    cookie.setPath(
                            "/"); // Important: set the path to match the cookie's path
                    response.addCookie(cookie);
                    log.info("Remember-me cookie cleared.");
                    break;
                }
            }
        }
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "Logged out successfully");
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/reset-password-request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        if (email == null || email.isEmpty()) {
            return new ResponseEntity<>(createErrorResponse("Email is " +
                    "required"), HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("Password reset request for non-existent email: {}",
                    email);
            // Return a generic success message to prevent email enumeration
            return new ResponseEntity<>(
                    createSuccessResponse("A password reset link has " +
                            "been " +
                            "sent."), HttpStatus.OK);
        }

        String resetToken = UUID.randomUUID().toString();
        user.setActivationCode(resetToken);
        user.setActivationCodeExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String resetLink =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/auth/reset-password")
                        .queryParam("token", resetToken)
                        .toUriString();

        // Create a subject and body for the email
        String emailSubject = "Paladin - Password Reset Request";
        String emailBody = String.format(
                "Dear %s,\n\nPlease use the following link to reset your password: %s",
                user.getFirstName(), resetLink);

        emailService.sendVerificationEmail(user.getEmail(), emailSubject,
                emailBody);

        log.info("Password reset link sent to {}", user.getEmail());
        return new ResponseEntity<>(
                createSuccessResponse("If an account with" +
                        " that email exists, a password reset link has been sent."),
                HttpStatus.OK);
    }


    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestParam String token,
            @RequestBody PasswordDto passwordDto) {
        try {
            User user =
                    userRepository.findByActivationCode(token)
                            .orElse(null);

            if (user == null || user.getActivationCodeExpiry() == null || user.getActivationCodeExpiry()
                    .isBefore(LocalDateTime.now())) {
                log.warn(
                        "Password reset failed: Invalid or expired token {}.",
                        token);
                return new ResponseEntity<>(
                        createErrorResponse("Invalid or " +
                                "expired password reset link."),
                        HttpStatus.BAD_REQUEST);
            }

            if (passwordDto.getPassword() == null || passwordDto.getPassword()
                    .length() < 8) {
                return new ResponseEntity<>(createErrorResponse(
                        "Password must be at least 8 characters long."),
                        HttpStatus.BAD_REQUEST);
            }

            user.setPassword(
                    passwordEncoder.encode(passwordDto.getPassword()));
            user.setActivationCode(null);
            user.setActivationCodeExpiry(null);
            userRepository.save(user);

            log.info("Password successfully reset for user with token {}.",
                    token);
            return new ResponseEntity<>(
                    createSuccessResponse("Password reset" +
                            " successfully."), HttpStatus.OK);
        } catch (Exception e) {
            log.error(
                    "An unexpected error occurred during password reset " +
                            "for token {}: {}", token, e.getMessage(), e);
            return new ResponseEntity<>(
                    createErrorResponse("An unexpected " +
                            "error occurred during password reset."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/set-password")
    public ResponseEntity<Map<String, String>> setPassword(
            @RequestBody PasswordDto passwordDto,
            Principal principal
    ) {
        AuthResult result = getAuthenticatedUser(principal);

        if (!result.isSuccess()) {
            return ResponseEntity.status(result.getStatus())
                    .body(Map.of("error", result.getErrorMessage()));
        }

        User user = result.getUser();
        user.setPassword(
                passwordEncoder.encode(passwordDto.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(
                Map.of("message", "Password set successfully!"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Not authenticated"));
        }

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(
                        () -> new UserNotFoundException("User with " +
                                "such email address not found")
                );

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("User not found"));
        }

        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        return ResponseEntity.ok(Map.of("user", userResponse));
    }

    private AuthResult getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            return AuthResult.unauthorized();
        }

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(
                        () -> new UserNotFoundException("User with " +
                                "such email address not found")
                );
        if (user == null) {
            return AuthResult.userNotFound();
        }

        return AuthResult.success(user);
    }

    private static Map<String, String> createErrorResponse(
            String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return errorResponse;
    }

    private static Map<String, String> createSuccessResponse(
            String message) {
        Map<String, String> successResponse = new HashMap<>();
        successResponse.put("message", message);
        return successResponse;
    }

    @Getter
    public static class AuthResult {
        private final boolean success;
        private final User user;
        private final HttpStatus status;
        private final String errorMessage;

        private AuthResult(boolean success, User user, HttpStatus status,
                           String errorMessage) {
            this.success = success;
            this.user = user;
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public static AuthResult success(User user) {
            return new AuthResult(true, user, HttpStatus.OK, null);
        }

        public static AuthResult unauthorized() {
            return new AuthResult(false, null, HttpStatus.UNAUTHORIZED,
                    "Not authenticated");
        }

        public static AuthResult userNotFound() {
            return new AuthResult(false, null, HttpStatus.NOT_FOUND,
                    "User not found");
        }
    }
}
