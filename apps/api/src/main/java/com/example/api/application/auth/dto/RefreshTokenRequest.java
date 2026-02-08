package com.example.api.application.auth.dto;

import java.util.Objects;

/**
 * Input DTO for refresh token use case.
 */
public record RefreshTokenRequest(String refreshToken) {

    public RefreshTokenRequest {
        Objects.requireNonNull(refreshToken, "Refresh token required");
        if (refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token required");
        }
    }
}
