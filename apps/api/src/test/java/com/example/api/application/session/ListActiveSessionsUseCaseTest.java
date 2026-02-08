package com.example.api.application.session;

import com.example.api.domain.session.Session;
import com.example.api.domain.session.SessionRepository;
import com.example.api.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListActiveSessionsUseCaseTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private ListActiveSessionsUseCase useCase;

    @Test
    void returnsActiveSessions() {
        final var session = Session.create(UserId.of("user-id"), Instant.now().plusSeconds(3600));
        when(sessionRepository.findActiveSessionsByUserId(UserId.of("user-id")))
                .thenReturn(List.of(session));

        final var response = useCase.execute("user-id");

        assertEquals(1, response.size());
        assertEquals(session.getId().value(), response.getFirst().id());
    }
}
