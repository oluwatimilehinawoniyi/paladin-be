package com.paladin.user.model;

import com.paladin.user.interfaces.OAuth2UserInfo;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId
            , Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "microsoft", "outlook" ->
                    new MicrosoftOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(
                    "Login with " + registrationId + " is not supported");
        };
    }
}
