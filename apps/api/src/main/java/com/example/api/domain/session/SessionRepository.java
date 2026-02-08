package com.example.api.domain.session;

import com.example.api.domain.user.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for session persistence.
 */
public interface SessionRepository {

    Optional<Session> findById(SessionId id);

    List<Session> findActiveSessionsByUserId(UserId userId);

    Session save(Session session);

    void delete(SessionId id);
}
