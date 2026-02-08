package com.example.api.application.auth.sdk.dto;

import java.util.Objects;

/**
 * Input DTO for SDK end user login.
 */
public record SdkLoginRequest(String email, String password) {

    public SdkLoginRequest {
        Objects.requireNonNull(email, "Email required");
        Objects.requireNonNull(password, "Password required");
    }
}
