package com.paladin.user.model;

import com.paladin.user.interfaces.OAuth2UserInfo;

import java.util.Map;

class MicrosoftOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public MicrosoftOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("displayName");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("mail");
    }

    @Override
    public String getFirstName() {
        return (String) attributes.get("givenName");
    }

    @Override
    public String getLastName() {
        return (String) attributes.get("surname");
    }

    @Override
    public String getNameAttributeKey() {
        return "id";
    }
}
