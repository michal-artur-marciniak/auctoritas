package com.example.api.application.auth.sdk.dto;

import java.util.Objects;

/**
 * Input DTO for SDK end user registration.
 */
public record SdkRegisterRequest(String email, String password, String name) {

    public SdkRegisterRequest {
        Objects.requireNonNull(email, "Email required");
        Objects.requireNonNull(password, "Password required");
        Objects.requireNonNull(name, "Name required");
    }
}
