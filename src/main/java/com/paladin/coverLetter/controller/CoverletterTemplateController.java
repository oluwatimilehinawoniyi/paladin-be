package com.paladin.coverLetter.controller;

import com.paladin.coverLetter.service.impl.CoverLetterServiceImpl;
import com.paladin.common.dto.UserDTO;
import com.paladin.common.exceptions.UserNotFoundException;
import com.paladin.common.response.ResponseHandler;
import com.paladin.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/cover-letters")
@RequiredArgsConstructor
public class CoverletterTemplateController {

    private final CoverLetterServiceImpl coverLetterService;
    private final UserService userService;

    @CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
    @GetMapping("/generate/{category}")
    public ResponseEntity<Object> generate(
            @PathVariable String category,
            @RequestParam(required = false) String candidateName,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String position,
            Principal principal) {
        getUserIdFromPrincipal(principal);

        String coverLetter = coverLetterService.generate(category, candidateName, companyName, position);

        return ResponseHandler.responseBuilder(
                "Cover letter successfully returned",
                HttpStatus.OK,
                coverLetter);
    }


    private void getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized: No principal found");
        }

        if (principal instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            String userEmail = oauth2User.getAttribute("email");

            if (userEmail == null) {
                throw new RuntimeException("Email not found in OAuth2 user attributes");
            }

            UserDTO user = userService.getUserByEmail(userEmail);
            if (user == null) {
                throw new UserNotFoundException(
                        "User not found for authenticated email: " + userEmail);
            }
            return;
        }

        String userEmail = principal.getName();
        UserDTO user = userService.getUserByEmail(userEmail);
        if (user == null) {
            throw new UserNotFoundException(
                    "User not found for authenticated principal: " + userEmail);
        }
    }

}
