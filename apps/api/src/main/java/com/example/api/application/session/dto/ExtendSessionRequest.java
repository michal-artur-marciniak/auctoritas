package com.example.api.application.session.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Input DTO for extending a session.
 */
public record ExtendSessionRequest(String sessionId, String userId, Instant expiresAt) {

    public ExtendSessionRequest {
        Objects.requireNonNull(sessionId, "Session ID required");
        Objects.requireNonNull(userId, "User ID required");
        Objects.requireNonNull(expiresAt, "Expiry timestamp required");
        if (sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID required");
        }
        if (userId.isBlank()) {
            throw new IllegalArgumentException("User ID required");
        }
    }
}
