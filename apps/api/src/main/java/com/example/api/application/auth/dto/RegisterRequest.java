package com.example.api.application.auth.dto;

import java.util.Objects;

/**
 * Input DTO for user registration use case.
 */
public record RegisterRequest(String email, String password, String name) {

    public RegisterRequest {
        Objects.requireNonNull(email, "Email required");
        Objects.requireNonNull(password, "Password required");
        Objects.requireNonNull(name, "Name required");
    }
}
