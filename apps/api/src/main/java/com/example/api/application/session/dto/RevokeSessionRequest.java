package com.example.api.application.session.dto;

import java.util.Objects;

/**
 * Input DTO for revoking a session.
 */
public record RevokeSessionRequest(String sessionId, String userId) {

    public RevokeSessionRequest {
        Objects.requireNonNull(sessionId, "Session ID required");
        Objects.requireNonNull(userId, "User ID required");
        if (sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID required");
        }
        if (userId.isBlank()) {
            throw new IllegalArgumentException("User ID required");
        }
    }
}
