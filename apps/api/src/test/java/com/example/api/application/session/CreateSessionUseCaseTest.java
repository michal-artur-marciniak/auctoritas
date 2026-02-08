package com.example.api.application.session;

import com.example.api.application.session.dto.CreateSessionRequest;
import com.example.api.domain.session.Session;
import com.example.api.domain.session.SessionRepository;
import com.example.api.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateSessionUseCaseTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private CreateSessionUseCase useCase;

    @Test
    void createsSessionForUser() {
        final var expiresAt = Instant.now().plusSeconds(3600);
        final var request = new CreateSessionRequest("user-id", expiresAt);

        useCase.execute(request);

        final var captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        final var session = captor.getValue();
        assertEquals(UserId.of("user-id"), session.getUserId());
        assertEquals(expiresAt, session.getExpiresAt());
    }
}
