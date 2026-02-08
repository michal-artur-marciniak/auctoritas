package com.example.api.application.session.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Input DTO for session creation.
 */
public record CreateSessionRequest(String userId, Instant expiresAt) {

    public CreateSessionRequest {
        Objects.requireNonNull(userId, "User ID required");
        Objects.requireNonNull(expiresAt, "Expiry timestamp required");
        if (userId.isBlank()) {
            throw new IllegalArgumentException("User ID required");
        }
    }
}
