package com.example.api.domain.session;

import com.example.api.domain.user.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing an authentication session.
 */
public class Session {

    private final SessionId id;
    private final UserId userId;
    private final Instant createdAt;
    private Instant expiresAt;
    private boolean revoked;

    public Session(SessionId id, UserId userId, Instant createdAt, Instant expiresAt, boolean revoked) {
        this.id = Objects.requireNonNull(id, "Session ID required");
        this.userId = Objects.requireNonNull(userId, "User ID required");
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiry timestamp required");
        this.revoked = revoked;
    }

    /**
     * Creates a new session with an expiration timestamp.
     */
    public static Session create(UserId userId, Instant expiresAt) {
        return new Session(SessionId.generate(), userId, Instant.now(), expiresAt, false);
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isActive(Instant now) {
        return !revoked && expiresAt.isAfter(now);
    }

    public void extendExpiry(Instant newExpiry) {
        this.expiresAt = Objects.requireNonNull(newExpiry, "Expiry timestamp required");
    }

    public SessionId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
