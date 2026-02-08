package com.example.api.presentation.session.dto;

import com.example.api.application.session.dto.SessionDto;

import java.time.Instant;

/**
 * REST response DTO for session data.
 */
public record SessionResponseDto(String id, Instant createdAt, Instant expiresAt, boolean revoked) {

    public static SessionResponseDto fromApplication(SessionDto session) {
        return new SessionResponseDto(
                session.id(),
                session.createdAt(),
                session.expiresAt(),
                session.revoked()
        );
    }
}
