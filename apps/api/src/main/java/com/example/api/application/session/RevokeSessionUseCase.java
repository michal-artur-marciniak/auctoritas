package com.example.api.application.session;

import com.example.api.application.session.dto.RevokeSessionRequest;
import com.example.api.domain.session.SessionId;
import com.example.api.domain.session.SessionRepository;
import com.example.api.domain.session.exception.SessionNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Use case for revoking a session.
 */
@Component
public class RevokeSessionUseCase {

    private final SessionRepository sessionRepository;

    public RevokeSessionUseCase(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public void execute(RevokeSessionRequest request) {
        final var session = sessionRepository.findById(SessionId.of(request.sessionId()))
                .orElseThrow(() -> new SessionNotFoundException(request.sessionId()));

        if (!session.getUserId().value().equals(request.userId())) {
            throw new SessionNotFoundException(request.sessionId());
        }

        session.revoke();
        sessionRepository.save(session);
    }
}
