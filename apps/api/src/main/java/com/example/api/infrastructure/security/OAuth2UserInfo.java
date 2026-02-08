package com.example.api.infrastructure.security;

import java.util.Objects;

/**
 * Normalized OAuth user profile.
 */
public record OAuth2UserInfo(String email, String name) {

    public OAuth2UserInfo {
        Objects.requireNonNull(email, "Email required");
        Objects.requireNonNull(name, "Name required");
    }
}
