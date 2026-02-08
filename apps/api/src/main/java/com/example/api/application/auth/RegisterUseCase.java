package com.example.api.application.auth;

import com.example.api.application.auth.dto.AuthResponse;
import com.example.api.application.auth.dto.RegisterRequest;
import com.example.api.application.auth.dto.UserDto;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import com.example.api.domain.user.User;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.EmailAlreadyExistsException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for registering a new user.
 */
@Component
public class RegisterUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public RegisterUseCase(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Registers a new user and returns an auth token.
     *
     * @param request the registration data
     * @return authentication response with token and user info
     * @throws EmailAlreadyExistsException if the email is already registered
     */
    @Transactional
    public AuthResponse execute(RegisterRequest request) {
        final var email = new Email(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(request.email());
        }

        final var user = User.register(
                email,
                Password.create(request.password(), passwordEncoder),
                request.name()
        );

        userRepository.save(user);

        final var accessToken = tokenProvider.generateAccessToken(user);
        final var refreshToken = tokenProvider.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, UserDto.fromDomain(user));
    }
}
