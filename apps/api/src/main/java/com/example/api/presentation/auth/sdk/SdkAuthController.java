package com.example.api.presentation.auth.sdk;

import com.example.api.application.auth.sdk.LoginEndUserUseCase;
import com.example.api.application.auth.sdk.RegisterEndUserUseCase;
import com.example.api.application.auth.sdk.dto.SdkAuthResponse;
import com.example.api.application.auth.sdk.dto.SdkLoginRequest;
import com.example.api.application.auth.sdk.dto.SdkRegisterRequest;
import com.example.api.infrastructure.security.ApiKeyAuthenticationFilter;
import com.example.api.infrastructure.security.ProjectContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * REST controller for SDK authentication endpoints.
 * These endpoints require X-API-Key header for project/environment scoping.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class SdkAuthController {

    private final RegisterEndUserUseCase registerEndUserUseCase;
    private final LoginEndUserUseCase loginEndUserUseCase;

    public SdkAuthController(RegisterEndUserUseCase registerEndUserUseCase,
                             LoginEndUserUseCase loginEndUserUseCase) {
        this.registerEndUserUseCase = registerEndUserUseCase;
        this.loginEndUserUseCase = loginEndUserUseCase;
    }

    /**
     * Registers a new SDK end user scoped to the project/environment from the API key.
     *
     * @param dto the registration request
     * @param request the HTTP request containing project context
     * @return user profile with SDK JWT
     */
    @PostMapping("/register")
    public ResponseEntity<SdkAuthResponse> register(
            @Valid @RequestBody SdkRegisterRequestDto dto,
            HttpServletRequest request) {
        final var context = getContextOrThrow(request);
        final var response = registerEndUserUseCase.execute(
                dto.toRequest(),
                context.projectId(),
                context.environmentId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates an SDK end user scoped to the project/environment from the API key.
     *
     * @param dto the login request
     * @param request the HTTP request containing project context
     * @return user profile with SDK JWT
     */
    @PostMapping("/login")
    public ResponseEntity<SdkAuthResponse> login(
            @Valid @RequestBody SdkLoginRequestDto dto,
            HttpServletRequest request) {
        final var context = getContextOrThrow(request);
        final var response = loginEndUserUseCase.execute(
                dto.toRequest(),
                context.projectId(),
                context.environmentId()
        );
        return ResponseEntity.ok(response);
    }

    private ProjectContext getContextOrThrow(HttpServletRequest request) {
        final var context = ApiKeyAuthenticationFilter.getContext(request);
        if (context == null) {
            throw new MissingApiKeyException();
        }
        return context;
    }
}
