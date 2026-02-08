package com.example.api.application.session.dto;

import com.example.api.domain.session.Session;

import java.time.Instant;

/**
 * Output DTO for session data.
 */
public record SessionDto(String id, String userId, Instant createdAt, Instant expiresAt, boolean revoked) {

    public static SessionDto fromDomain(Session session) {
        return new SessionDto(
                session.getId().value(),
                session.getUserId().value(),
                session.getCreatedAt(),
                session.getExpiresAt(),
                session.isRevoked()
        );
    }
}
