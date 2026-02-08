package com.example.api.application.auth;

import com.example.api.application.auth.dto.AuthResponse;
import com.example.api.application.auth.dto.RefreshTokenRequest;
import com.example.api.application.auth.dto.UserDto;
import com.example.api.domain.user.UserId;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.InvalidCredentialsException;
import com.example.api.domain.user.exception.UserNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Use case for refreshing an access token.
 */
@Component
public class RefreshTokenUseCase {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final AuthSessionService authSessionService;

    public RefreshTokenUseCase(UserRepository userRepository,
                               TokenProvider tokenProvider,
                               AuthSessionService authSessionService) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.authSessionService = authSessionService;
    }

    /**
     * Validates a refresh token and issues a new access token.
     */
    public AuthResponse execute(RefreshTokenRequest request) {
        final var refreshToken = request.refreshToken();
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException();
        }

        final var userId = tokenProvider.getUserIdFromToken(refreshToken);
        final var user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new UserNotFoundException(userId));

        final var accessToken = tokenProvider.generateAccessToken(user);
        authSessionService.extendSession(user);
        return new AuthResponse(accessToken, refreshToken, UserDto.fromDomain(user));
    }
}
