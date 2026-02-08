package com.example.api.application.auth;

import com.example.api.application.auth.dto.AuthResponse;
import com.example.api.application.auth.dto.LoginRequest;
import com.example.api.application.auth.dto.UserDto;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.PasswordEncoder;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.InvalidCredentialsException;
import com.example.api.domain.user.exception.UserBannedException;
import org.springframework.stereotype.Component;

/**
 * Use case for user login.
 */
@Component
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final AuthSessionService authSessionService;

    public LoginUseCase(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        TokenProvider tokenProvider,
                        AuthSessionService authSessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authSessionService = authSessionService;
    }

    /**
     * Authenticates a user and returns an auth token.
     *
     * @param request the login credentials
     * @return authentication response with token and user info
     * @throws InvalidCredentialsException if email/password don't match
     * @throws UserBannedException if the user is banned
     */
    public AuthResponse execute(LoginRequest request) {
        final var email = new Email(request.email());

        final var user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.canLogin()) {
            throw new UserBannedException();
        }

        if (!user.getPassword().matches(request.password(), passwordEncoder)) {
            throw new InvalidCredentialsException();
        }

        final var accessToken = tokenProvider.generateAccessToken(user);
        final var refreshToken = tokenProvider.generateRefreshToken(user);
        authSessionService.createSession(user);
        return new AuthResponse(accessToken, refreshToken, UserDto.fromDomain(user));
    }
}
