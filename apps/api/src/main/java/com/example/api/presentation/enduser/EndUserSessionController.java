package com.example.api.presentation.enduser;

import com.example.api.application.session.CreateSessionUseCase;
import com.example.api.application.session.ExtendSessionUseCase;
import com.example.api.application.session.ListActiveSessionsUseCase;
import com.example.api.application.session.RevokeSessionUseCase;
import com.example.api.application.session.dto.RevokeSessionRequest;
import com.example.api.infrastructure.security.ApiKeyAuthenticationFilter;
import com.example.api.infrastructure.security.ProjectContext;
import com.example.api.presentation.session.dto.CreateSessionRequestDto;
import com.example.api.presentation.session.dto.ExtendSessionRequestDto;
import com.example.api.presentation.session.dto.SessionResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * REST controller for end user session management endpoints.
 *
 * <p>These endpoints allow SDK end users to view and manage their active sessions
 * for account security. Sessions are scoped to the authenticated end user.</p>
 *
 * <p>Requirements:</p>
 * <ul>
 *   <li>SDK JWT authentication (from /api/v1/auth/login or /api/v1/auth/register)</li>
 *   <li>X-API-Key header for project context</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/end-users/sessions")
public class EndUserSessionController {

    private final CreateSessionUseCase createSessionUseCase;
    private final ExtendSessionUseCase extendSessionUseCase;
    private final RevokeSessionUseCase revokeSessionUseCase;
    private final ListActiveSessionsUseCase listActiveSessionsUseCase;

    public EndUserSessionController(CreateSessionUseCase createSessionUseCase,
                                    ExtendSessionUseCase extendSessionUseCase,
                                    RevokeSessionUseCase revokeSessionUseCase,
                                    ListActiveSessionsUseCase listActiveSessionsUseCase) {
        this.createSessionUseCase = createSessionUseCase;
        this.extendSessionUseCase = extendSessionUseCase;
        this.revokeSessionUseCase = revokeSessionUseCase;
        this.listActiveSessionsUseCase = listActiveSessionsUseCase;
    }

    /**
     * Lists all active sessions for the current end user.
     *
     * <p>Returns a list of sessions that belong to the authenticated user.
     * Sessions are scoped to the user - users can only see their own sessions.</p>
     *
     * @param jwt the authenticated user's JWT
     * @return list of active sessions
     */
    @GetMapping
    public ResponseEntity<List<SessionResponseDto>> listSessions(@AuthenticationPrincipal Jwt jwt) {
        final var userId = jwt.getSubject();
        final var sessions = listActiveSessionsUseCase.execute(userId).stream()
                .map(SessionResponseDto::fromApplication)
                .toList();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Creates a new session for the current end user.
     *
     * <p>Creates a new authentication session with the specified expiration time.
     * The session is associated with the authenticated user.</p>
     *
     * @param jwt the authenticated user's JWT
     * @param dto the session creation request containing expiry timestamp
     * @return the created session
     */
    @PostMapping
    public ResponseEntity<SessionResponseDto> createSession(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateSessionRequestDto dto) {
        final var userId = jwt.getSubject();
        final var session = createSessionUseCase.execute(dto.toRequest(userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SessionResponseDto.fromApplication(session));
    }

    /**
     * Extends the expiry of an existing session.
     *
     * <p>Updates the expiration timestamp of a session. The session must belong
     * to the authenticated user. Returns 404 if the session doesn't exist or
     * doesn't belong to the user.</p>
     *
     * @param jwt the authenticated user's JWT
     * @param sessionId the ID of the session to extend
     * @param dto the session extension request containing new expiry timestamp
     * @return the updated session
     */
    @PatchMapping("/{sessionId}")
    public ResponseEntity<SessionResponseDto> extendSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String sessionId,
            @Valid @RequestBody ExtendSessionRequestDto dto) {
        final var userId = jwt.getSubject();
        final var session = extendSessionUseCase.execute(dto.toRequest(sessionId, userId));
        return ResponseEntity.ok(SessionResponseDto.fromApplication(session));
    }

    /**
     * Revokes a session.
     *
     * <p>Marks a session as revoked, preventing it from being used for authentication.
     * The session must belong to the authenticated user. Returns 404 if the session
     * doesn't exist or doesn't belong to the user. Returns 204 on success.</p>
     *
     * @param jwt the authenticated user's JWT
     * @param sessionId the ID of the session to revoke
     * @return empty response on success
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String sessionId) {
        final var userId = jwt.getSubject();
        revokeSessionUseCase.execute(new RevokeSessionRequest(sessionId, userId));
        return ResponseEntity.noContent().build();
    }
}
