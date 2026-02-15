package com.example.api.presentation.enduser;

import com.example.api.application.auth.dto.UserDto;
import com.example.api.application.auth.sdk.GetCurrentSdkUserUseCase;
import com.example.api.application.auth.sdk.UpdateEndUserProfileUseCase;
import com.example.api.infrastructure.security.ApiKeyAuthenticationFilter;
import com.example.api.infrastructure.security.ProjectContext;
import com.example.api.presentation.enduser.dto.UpdateEndUserProfileRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * REST controller for end user profile management endpoints.
 *
 * <p>These endpoints require SDK JWT authentication and are scoped to a
 * specific project/environment via the API key context. Email uniqueness
 * is enforced within the project scope.</p>
 */
@RestController
@RequestMapping("/api/v1/end-users/me")
public class EndUserController {

    private final GetCurrentSdkUserUseCase getCurrentSdkUserUseCase;
    private final UpdateEndUserProfileUseCase updateEndUserProfileUseCase;

    public EndUserController(GetCurrentSdkUserUseCase getCurrentSdkUserUseCase,
                             UpdateEndUserProfileUseCase updateEndUserProfileUseCase) {
        this.getCurrentSdkUserUseCase = getCurrentSdkUserUseCase;
        this.updateEndUserProfileUseCase = updateEndUserProfileUseCase;
    }

    /**
     * Returns the current end user profile.
     *
     * <p>This endpoint enforces project-level isolation. The user is looked up
     * only within the project and environment scope from the API key used during
     * authentication. Cross-project access attempts return 404 Not Found.</p>
     *
     * @param jwt the authenticated user's JWT
     * @param request the HTTP request containing project context
     * @return user profile
     */
    @GetMapping
    public ResponseEntity<UserDto> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        final var userId = jwt.getSubject();
        final var context = getContextOrThrow(request);

        final var user = getCurrentSdkUserUseCase.execute(
                userId,
                context.projectId(),
                context.environmentId()
        );

        return ResponseEntity.ok(user);
    }

    /**
     * Updates the current end user's profile.
     *
     * <p>Email uniqueness is enforced within the project and environment scope.
     * If the email is already in use by another user in the same project,
     * a 409 Conflict is returned.</p>
     *
     * @param jwt the authenticated user's JWT
     * @param request the HTTP request containing project context
     * @param dto the profile update request
     * @return updated user profile
     */
    @PatchMapping
    public ResponseEntity<UserDto> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request,
            @Valid @RequestBody UpdateEndUserProfileRequestDto dto) {
        final var userId = jwt.getSubject();
        final var context = getContextOrThrow(request);

        final var user = updateEndUserProfileUseCase.execute(
                userId,
                context.projectId(),
                context.environmentId(),
                dto.email(),
                dto.name()
        );

        return ResponseEntity.ok(user);
    }

    private ProjectContext getContextOrThrow(HttpServletRequest request) {
        final var context = ApiKeyAuthenticationFilter.getContext(request);
        if (context == null) {
            throw new IllegalStateException("Project context not found");
        }
        return context;
    }
}
