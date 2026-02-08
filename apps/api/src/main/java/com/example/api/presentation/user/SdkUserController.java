package com.example.api.presentation.user;

import com.example.api.application.auth.dto.UserDto;
import com.example.api.application.auth.sdk.GetCurrentSdkUserUseCase;
import com.example.api.infrastructure.security.ApiKeyAuthenticationFilter;
import com.example.api.infrastructure.security.ProjectContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * REST controller for SDK user endpoints.
 *
 * <p>These endpoints require SDK JWT authentication and are scoped to a
 * project/environment via the API key context.</p>
 */
@RestController
@RequestMapping("/api/v1/users/me")
public class SdkUserController {

    private final GetCurrentSdkUserUseCase getCurrentSdkUserUseCase;

    public SdkUserController(GetCurrentSdkUserUseCase getCurrentSdkUserUseCase) {
        this.getCurrentSdkUserUseCase = getCurrentSdkUserUseCase;
    }

    /**
     * Returns the current SDK end user profile.
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

    private ProjectContext getContextOrThrow(HttpServletRequest request) {
        final var context = ApiKeyAuthenticationFilter.getContext(request);
        if (context == null) {
            throw new IllegalStateException("Project context not found");
        }
        return context;
    }
}
