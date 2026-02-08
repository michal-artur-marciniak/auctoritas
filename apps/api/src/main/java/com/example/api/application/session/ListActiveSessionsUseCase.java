package com.example.api.application.session;

import com.example.api.application.session.dto.SessionDto;
import com.example.api.domain.session.SessionRepository;
import com.example.api.domain.user.UserId;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Use case for listing active sessions for a user.
 */
@Component
public class ListActiveSessionsUseCase {

    private final SessionRepository sessionRepository;

    public ListActiveSessionsUseCase(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public List<SessionDto> execute(String userId) {
        return sessionRepository.findActiveSessionsByUserId(UserId.of(userId)).stream()
                .map(SessionDto::fromDomain)
                .toList();
    }
}
