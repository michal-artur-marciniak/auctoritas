package com.example.api.application.auth.dto;

import java.util.Objects;

/**
 * Input DTO for user login use case.
 */
public record LoginRequest(String email, String password) {

    public LoginRequest {
        Objects.requireNonNull(email, "Email required");
        Objects.requireNonNull(password, "Password required");
    }
}
