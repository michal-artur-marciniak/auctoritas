package com.example.api.presentation.auth;

import com.example.api.application.auth.LoginUseCase;
import com.example.api.application.auth.RefreshTokenUseCase;
import com.example.api.application.auth.RegisterUseCase;
import com.example.api.application.auth.dto.AuthResponse;
import com.example.api.application.auth.dto.RefreshTokenRequest;
import com.example.api.application.auth.dto.UserDto;
import com.example.api.infrastructure.security.AuthCookieService;
import com.example.api.domain.user.exception.InvalidCredentialsException;
import com.example.api.presentation.auth.dto.LoginRequestDto;
import com.example.api.presentation.auth.dto.RefreshTokenRequestDto;
import com.example.api.presentation.auth.dto.RegisterRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URI;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final AuthCookieService authCookieService;

    public AuthController(RegisterUseCase registerUseCase,
                          LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          AuthCookieService authCookieService) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.authCookieService = authCookieService;
    }

    /**
     * Registers a new user.
     *
     * @param dto the registration request
     * @return user profile data
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequestDto dto,
                                            HttpServletResponse response) {
        final var authResponse = registerUseCase.execute(dto.toRequest());
        applyAuthCookies(response, authResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse.user());
    }

    /**
     * Authenticates a user.
     *
     * @param dto the login request
     * @return user profile data
     */
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody LoginRequestDto dto,
                                         HttpServletResponse response) {
        final var authResponse = loginUseCase.execute(dto.toRequest());
        applyAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse.user());
    }

    /**
     * Refreshes an access token.
     *
     * @param dto the refresh token request
     * @return user profile data
     */
    @PostMapping("/refresh")
    public ResponseEntity<UserDto> refresh(@Valid @RequestBody(required = false) RefreshTokenRequestDto dto,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        final var refreshToken = resolveRefreshToken(dto, request);
        final var authResponse = refreshTokenUseCase.execute(new RefreshTokenRequest(refreshToken));
        applyAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse.user());
    }

    private void applyAuthCookies(HttpServletResponse servletResponse, AuthResponse authResponse) {
        authCookieService.setAccessToken(servletResponse, authResponse.accessToken());
        authCookieService.setRefreshToken(servletResponse, authResponse.refreshToken());
    }

    private String resolveRefreshToken(RefreshTokenRequestDto dto, HttpServletRequest request) {
        if (dto != null && dto.refreshToken() != null && !dto.refreshToken().isBlank()) {
            return dto.refreshToken();
        }

        final var token = authCookieService.readRefreshToken(request).orElse(null);
        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return token;
    }

    /**
     * Initiates OAuth login with GitHub.
     */
    @GetMapping("/oauth/github")
    public ResponseEntity<Void> oauthGithub() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/github"))
                .build();
    }

    /**
     * Initiates OAuth login with Google.
     */
    @GetMapping("/oauth/google")
    public ResponseEntity<Void> oauthGoogle() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/google"))
                .build();
    }
}
