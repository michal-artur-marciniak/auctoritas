package com.example.api.application.auth;

import com.example.api.domain.session.Session;
import com.example.api.domain.session.SessionRepository;
import com.example.api.domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Manages session persistence for auth flows.
 */
@Component
public class AuthSessionService {

    private final SessionRepository sessionRepository;
    private final long refreshExpirationMs;

    public AuthSessionService(SessionRepository sessionRepository,
                              @Value("${jwt.refresh-expiration}") long refreshExpirationMs) {
        this.sessionRepository = sessionRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public void createSession(User user) {
        final var expiresAt = Instant.now().plusMillis(refreshExpirationMs);
        sessionRepository.save(Session.create(user.getId(), expiresAt));
    }

    public void extendSession(User user) {
        final var expiresAt = Instant.now().plusMillis(refreshExpirationMs);
        sessionRepository.findActiveSessionsByUserId(user.getId()).stream()
                .findFirst()
                .ifPresentOrElse(session -> {
                    session.extendExpiry(expiresAt);
                    sessionRepository.save(session);
                }, () -> sessionRepository.save(Session.create(user.getId(), expiresAt)));
    }
}
