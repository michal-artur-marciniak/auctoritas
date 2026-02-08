package com.example.api.infrastructure.security;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps provider-specific OAuth2 attributes into a normalized profile.
 */
@Component
public class OAuth2UserInfoMapper {

    public OAuth2UserInfo fromOauthUser(String registrationId, OAuth2User user) {
        return switch (registrationId) {
            case "github" -> fromGithub(user.getAttributes());
            case "google" -> fromGoogle(user.getAttributes());
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + registrationId);
        };
    }

    private OAuth2UserInfo fromGithub(Map<String, Object> attributes) {
        final var email = (String) attributes.get("email");
        final var name = (String) attributes.getOrDefault("name", "GitHub User");
        if (email == null || email.isBlank()) {
            return new OAuth2UserInfo("", name);
        }
        return new OAuth2UserInfo(email, name);
    }

    private OAuth2UserInfo fromGoogle(Map<String, Object> attributes) {
        final var email = (String) attributes.get("email");
        final var name = (String) attributes.getOrDefault("name", "Google User");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account has no email");
        }
        return new OAuth2UserInfo(email, name);
    }
}
