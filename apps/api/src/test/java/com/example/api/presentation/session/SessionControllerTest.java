package com.example.api.presentation.session;

import com.example.api.application.session.CreateSessionUseCase;
import com.example.api.application.session.ExtendSessionUseCase;
import com.example.api.application.session.ListActiveSessionsUseCase;
import com.example.api.application.session.RevokeSessionUseCase;
import com.example.api.application.session.dto.SessionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionControllerTest {

    private MockMvc mockMvc;

    private CreateSessionUseCase createSessionUseCase;

    private ExtendSessionUseCase extendSessionUseCase;

    private RevokeSessionUseCase revokeSessionUseCase;

    private ListActiveSessionsUseCase listActiveSessionsUseCase;

    private SessionController controller;

    @BeforeEach
    void setUp() {
        createSessionUseCase = Mockito.mock(CreateSessionUseCase.class);
        extendSessionUseCase = Mockito.mock(ExtendSessionUseCase.class);
        revokeSessionUseCase = Mockito.mock(RevokeSessionUseCase.class);
        listActiveSessionsUseCase = Mockito.mock(ListActiveSessionsUseCase.class);
        controller = new SessionController(
                createSessionUseCase,
                extendSessionUseCase,
                revokeSessionUseCase,
                listActiveSessionsUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listSessionsReturnsActiveSessions() throws Exception {
        when(listActiveSessionsUseCase.execute("user-id"))
                .thenReturn(List.of(sessionDto("session-1")));

        mockMvc.perform(get("/api/sessions")
                        .principal(userAuth("user-id"))
                        .with(authentication(userAuth("user-id"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("session-1"));
    }

    @Test
    void createSessionReturnsCreated() throws Exception {
        when(createSessionUseCase.execute(any()))
                .thenReturn(sessionDto("session-1"));

        mockMvc.perform(post("/api/sessions")
                        .principal(userAuth("user-id"))
                        .with(authentication(userAuth("user-id")))
                        .contentType("application/json")
                        .content("{\"expiresAt\":\"2026-02-10T12:00:00Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("session-1"));
    }

    @Test
    void extendSessionReturnsOk() throws Exception {
        when(extendSessionUseCase.execute(any()))
                .thenReturn(sessionDto("session-1"));

        mockMvc.perform(patch("/api/sessions/session-1")
                        .principal(userAuth("user-id"))
                        .with(authentication(userAuth("user-id")))
                        .contentType("application/json")
                        .content("{\"expiresAt\":\"2026-02-10T12:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("session-1"));
    }

    @Test
    void revokeSessionReturnsNoContent() throws Exception {
        doNothing().when(revokeSessionUseCase).execute(any());

        mockMvc.perform(delete("/api/sessions/session-1")
                        .principal(userAuth("user-id"))
                        .with(authentication(userAuth("user-id"))))
                .andExpect(status().isNoContent());
    }

    private SessionDto sessionDto(String id) {
        return new SessionDto(id, "user-id", Instant.now(), Instant.now().plusSeconds(3600), false);
    }

    private UsernamePasswordAuthenticationToken userAuth(String userId) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                "",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
