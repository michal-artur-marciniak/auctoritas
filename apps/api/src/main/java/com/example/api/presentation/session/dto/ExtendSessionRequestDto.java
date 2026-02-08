package com.example.api.presentation.session.dto;

import com.example.api.application.session.dto.ExtendSessionRequest;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * REST request DTO for extending a session.
 */
public record ExtendSessionRequestDto(
        @NotNull(message = "Expiry timestamp is required")
        Instant expiresAt
) {

    public ExtendSessionRequest toRequest(String sessionId, String userId) {
        return new ExtendSessionRequest(sessionId, userId, expiresAt);
    }
}
