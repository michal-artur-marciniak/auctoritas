package com.example.api.infrastructure.persistence;

import com.example.api.domain.session.Session;
import com.example.api.domain.session.SessionId;
import com.example.api.domain.user.UserId;

/**
 * Maps between the domain {@link Session} and the JPA {@link SessionJpaEntity}.
 */
final class SessionDomainMapper {

    private SessionDomainMapper() {
        // Utility class
    }

    static Session toDomain(SessionJpaEntity entity) {
        return new Session(
                SessionId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.isRevoked()
        );
    }

    static SessionJpaEntity toEntity(Session session) {
        return new SessionJpaEntity(
                session.getId().value(),
                session.getUserId().value(),
                session.getCreatedAt(),
                session.getExpiresAt(),
                session.isRevoked()
        );
    }
}
