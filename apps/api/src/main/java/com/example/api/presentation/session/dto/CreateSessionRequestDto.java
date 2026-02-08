package com.example.api.presentation.session.dto;

import com.example.api.application.session.dto.CreateSessionRequest;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * REST request DTO for creating a session.
 */
public record CreateSessionRequestDto(
        @NotNull(message = "Expiry timestamp is required")
        Instant expiresAt
) {

    public CreateSessionRequest toRequest(String userId) {
        return new CreateSessionRequest(userId, expiresAt);
    }
}
