package com.example.api.application.session;

import com.example.api.application.session.dto.ExtendSessionRequest;
import com.example.api.application.session.dto.SessionDto;
import com.example.api.domain.session.SessionRepository;
import com.example.api.domain.session.SessionId;
import com.example.api.domain.session.exception.SessionNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Use case for extending a session.
 */
@Component
public class ExtendSessionUseCase {

    private final SessionRepository sessionRepository;

    public ExtendSessionUseCase(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public SessionDto execute(ExtendSessionRequest request) {
        final var session = sessionRepository.findById(SessionId.of(request.sessionId()))
                .orElseThrow(() -> new SessionNotFoundException(request.sessionId()));

        if (!session.getUserId().value().equals(request.userId())) {
            throw new SessionNotFoundException(request.sessionId());
        }

        session.extendExpiry(request.expiresAt());
        sessionRepository.save(session);

        return SessionDto.fromDomain(session);
    }
}
