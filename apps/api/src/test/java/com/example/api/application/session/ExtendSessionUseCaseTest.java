package com.example.api.application.session;

import com.example.api.application.session.dto.ExtendSessionRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtendSessionUseCaseTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private ExtendSessionUseCase useCase;

    @Test
    void extendsSessionExpiry() {
        final var session = new Session(
                SessionId.of("session-id"),
                UserId.of("user-id"),
                Instant.now(),
                Instant.now().plusSeconds(60),
                false
        );
        when(sessionRepository.findById(SessionId.of("session-id")))
                .thenReturn(Optional.of(session));

        final var newExpiry = Instant.now().plusSeconds(3600);
        final var request = new ExtendSessionRequest("session-id", "user-id", newExpiry);

        final var response = useCase.execute(request);

        assertEquals(newExpiry, response.expiresAt());
        verify(sessionRepository).save(session);
    }
}
