package com.example.api.application.session;

import com.example.api.application.session.dto.CreateSessionRequest;
import com.example.api.application.session.dto.SessionDto;
import com.example.api.domain.session.Session;
import com.example.api.domain.session.SessionRepository;
import com.example.api.domain.user.UserId;
import org.springframework.stereotype.Component;

/**
 * Use case for creating a new session.
 */
@Component
public class CreateSessionUseCase {

    private final SessionRepository sessionRepository;

    public CreateSessionUseCase(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public SessionDto execute(CreateSessionRequest request) {
        final var session = Session.create(UserId.of(request.userId()), request.expiresAt());
        sessionRepository.save(session);
        return SessionDto.fromDomain(session);
    }
}
