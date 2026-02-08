package com.example.api.application.session;

import com.example.api.application.session.dto.RevokeSessionRequest;
import com.example.api.domain.session.Session;
import com.example.api.domain.session.SessionId;
import com.example.api.domain.session.SessionRepository;
import com.example.api.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeSessionUseCaseTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private RevokeSessionUseCase useCase;

    @Test
    void revokesSession() {
        final var session = new Session(
                SessionId.of("session-id"),
                UserId.of("user-id"),
                Instant.now(),
                Instant.now().plusSeconds(60),
                false
        );
        when(sessionRepository.findById(SessionId.of("session-id")))
                .thenReturn(Optional.of(session));

        useCase.execute(new RevokeSessionRequest("session-id", "user-id"));

        assertTrue(session.isRevoked());
        verify(sessionRepository).save(session);
    }
}
