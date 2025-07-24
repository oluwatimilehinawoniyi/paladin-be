package com.paladin.auth;

import com.paladin.auth.service.EmailService;
import com.paladin.dto.*;
import com.paladin.enums.AuthProvider;
import com.paladin.exceptions.UserNotFoundException;
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
            @RequestBody @Valid UserRegisterRequestDTO request) {
        Map<String, String> response = new HashMap<>();
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            response.put("error", "User already exists!");
            return ResponseEntity.badRequest().body(response);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);
        user.setActivationCode(UUID.randomUUID().toString());
        user.setActivationCodeExpiry(LocalDateTime.now().plusMinutes(10));

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
                        
                        This link \
                        will expire in 10 minutes.
                        
                        Thank you,\
                        
                        Paladin Team""",
                user.getFirstName(),
                verificationUrl);

        try {
            emailService.sendEmail(user.getEmail(), emailSubject,
                    emailBody);
            response.put("message", "User registered successfully!");
            return ResponseEntity.ok(response);
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
        Map<String, String> response = new HashMap<>();
        User user = userRepository.findByActivationCode(token).orElseThrow(
                () -> new UserNotFoundException("User not found for " +
                        "token: " + token + ".")
        );
        if (user == null) {
            response.put("error", "Invalid verification code!");
            return ResponseEntity.badRequest().body(response);
        }

        if (user.getActivationCodeExpiry().isBefore(LocalDateTime.now())) {
            response.put("error", "Verification code has expired!. " +
                    "Please register again.");
            return ResponseEntity.badRequest().body(response);
        }

        if (user.isEmailVerified()) {
            response.put("message", "Email already verified!");
            return ResponseEntity.ok(response);
        }

        user.setEmailVerified(true);
        user.setActivationCode(null);
        user.setActivationCodeExpiry(null);
        userRepository.save(user);

        response.put("message", "Email verified successfully! You can " +
                "log in now!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
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

            response.put("message", "Login successful!");
            response.put("sessionId", session.getId());
            response.put("user", new UserResponseDTO() {{
                setId(Objects.requireNonNull(user).getId());
                setEmail(user.getEmail());
                setFirstName(user.getFirstName());
                setLastName(user.getLastName());
            }});

            return ResponseEntity.ok(response);

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
            HttpServletRequest request, HttpServletResponse response) {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            try {
                rememberMeServices.loginFail(request, response);
                System.out.println("Remember-me cookie cleared");
            } catch (Exception e) {
                System.err.println(
                        "Error clearing remember-me cookie: " + e.getMessage());
            }
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        Cookie cookie = new Cookie("SESSIONID", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true in prod
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        Cookie rememberMeCookie = new Cookie("REMEMBER_ME_COOKIE", null);
        rememberMeCookie.setHttpOnly(true);
        rememberMeCookie.setSecure(false); // true in prod
        rememberMeCookie.setPath("/");
        rememberMeCookie.setMaxAge(0);
        response.addCookie(rememberMeCookie);

        return ResponseEntity.ok(
                Map.of("message", "Logged out successfully!"));
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
    public ResponseEntity<?> getCurrentUser(
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

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(
            Principal principal) {

        Map<String, Object> status = new HashMap<>();

        if (principal != null) {
            status.put("authenticated", true);
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(
                            () -> new UserNotFoundException("User with " +
                                    "such email address not found")
                    );
            if (user != null) {
                UserResponseDTO userResponse = new UserResponseDTO();
                userResponse.setId(user.getId());
                userResponse.setEmail(user.getEmail());
                userResponse.setFirstName(user.getFirstName());
                userResponse.setLastName(user.getLastName());
                status.put("user", userResponse);
            }
        } else {
            status.put("authenticated", false);
        }

        return ResponseEntity.ok(status);
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
