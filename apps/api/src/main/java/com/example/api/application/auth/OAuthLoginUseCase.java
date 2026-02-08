package com.example.api.application.auth;

import com.example.api.application.auth.dto.AuthResponse;
import com.example.api.application.auth.dto.UserDto;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import com.example.api.domain.user.User;
import com.example.api.domain.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for OAuth login/register flows.
 */
@Component
public class OAuthLoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final AuthSessionService authSessionService;

    public OAuthLoginUseCase(UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             TokenProvider tokenProvider,
                             AuthSessionService authSessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authSessionService = authSessionService;
    }

    /**
     * Finds or creates a user by OAuth email and returns tokens.
     */
    @Transactional
    public AuthResponse execute(String email, String name) {
        final var normalizedEmail = new Email(email);
        final var user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> createUser(normalizedEmail, name));

        final var accessToken = tokenProvider.generateAccessToken(user);
        final var refreshToken = tokenProvider.generateRefreshToken(user);
        authSessionService.createSession(user);
        return new AuthResponse(accessToken, refreshToken, UserDto.fromDomain(user));
    }

    private User createUser(Email email, String name) {
        final var placeholderPassword = UUID.randomUUID().toString() + UUID.randomUUID();
        final var user = User.register(email, Password.create(placeholderPassword, passwordEncoder), name);
        return userRepository.save(user);
    }
}
