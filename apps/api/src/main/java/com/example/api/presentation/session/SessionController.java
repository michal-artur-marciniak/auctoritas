package com.example.api.presentation.session;

import com.example.api.application.session.CreateSessionUseCase;
import com.example.api.application.session.ExtendSessionUseCase;
import com.example.api.application.session.ListActiveSessionsUseCase;
import com.example.api.application.session.RevokeSessionUseCase;
import com.example.api.application.session.dto.RevokeSessionRequest;
import com.example.api.presentation.session.dto.CreateSessionRequestDto;
import com.example.api.presentation.session.dto.ExtendSessionRequestDto;
import com.example.api.presentation.session.dto.SessionResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for session lifecycle endpoints.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final CreateSessionUseCase createSessionUseCase;
    private final ExtendSessionUseCase extendSessionUseCase;
    private final RevokeSessionUseCase revokeSessionUseCase;
    private final ListActiveSessionsUseCase listActiveSessionsUseCase;

    public SessionController(CreateSessionUseCase createSessionUseCase,
                             ExtendSessionUseCase extendSessionUseCase,
                             RevokeSessionUseCase revokeSessionUseCase,
                             ListActiveSessionsUseCase listActiveSessionsUseCase) {
        this.createSessionUseCase = createSessionUseCase;
        this.extendSessionUseCase = extendSessionUseCase;
        this.revokeSessionUseCase = revokeSessionUseCase;
        this.listActiveSessionsUseCase = listActiveSessionsUseCase;
    }

    @PostMapping
    public ResponseEntity<SessionResponseDto> createSession(
            Authentication authentication,
            @Valid @RequestBody CreateSessionRequestDto dto) {
        final var userId = (String) authentication.getPrincipal();
        final var session = createSessionUseCase.execute(dto.toRequest(userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SessionResponseDto.fromApplication(session));
    }

    @GetMapping
    public ResponseEntity<List<SessionResponseDto>> listSessions(Authentication authentication) {
        final var userId = (String) authentication.getPrincipal();
        final var sessions = listActiveSessionsUseCase.execute(userId).stream()
                .map(SessionResponseDto::fromApplication)
                .toList();
        return ResponseEntity.ok(sessions);
    }

    @PatchMapping("/{sessionId}")
    public ResponseEntity<SessionResponseDto> extendSession(
            Authentication authentication,
            @PathVariable String sessionId,
            @Valid @RequestBody ExtendSessionRequestDto dto) {
        final var userId = (String) authentication.getPrincipal();
        final var session = extendSessionUseCase.execute(dto.toRequest(sessionId, userId));
        return ResponseEntity.ok(SessionResponseDto.fromApplication(session));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            Authentication authentication,
            @PathVariable String sessionId) {
        final var userId = (String) authentication.getPrincipal();
        revokeSessionUseCase.execute(new RevokeSessionRequest(sessionId, userId));
        return ResponseEntity.noContent().build();
    }
}
