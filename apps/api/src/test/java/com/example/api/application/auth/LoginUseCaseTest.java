package com.example.api.application.auth;

import com.example.api.application.auth.dto.LoginRequest;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import com.example.api.domain.user.Role;
import com.example.api.domain.user.User;
import com.example.api.domain.user.UserId;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.InvalidCredentialsException;
import com.example.api.domain.user.exception.UserBannedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private AuthSessionService authSessionService;

    @InjectMocks
    private LoginUseCase useCase;

    @Test
    void rejectsInvalidPassword() {
        final var user = sampleUser();
        when(userRepository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", user.getPassword().hashedValue())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
                useCase.execute(new LoginRequest("user@example.com", "bad")));

        verify(authSessionService, never()).createSession(user);
    }

    @Test
    void rejectsBannedUser() {
        final var user = new User(
                UserId.of("user-id"),
                new Email("user@example.com"),
                Password.fromHash("hashed"),
                "User",
                Role.USER,
                true,
                "reason",
                null,
                LocalDateTime.now()
        );
        when(userRepository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(user));

        assertThrows(UserBannedException.class, () ->
                useCase.execute(new LoginRequest("user@example.com", "password")));

        verify(authSessionService, never()).createSession(user);
    }

    @Test
    void createsSessionOnSuccess() {
        final var user = sampleUser();
        when(userRepository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPassword().hashedValue())).thenReturn(true);
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        final var response = useCase.execute(new LoginRequest("user@example.com", "password"));

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        verify(authSessionService).createSession(user);
    }

    private User sampleUser() {
        return new User(
                UserId.of("user-id"),
                new Email("user@example.com"),
                Password.fromHash("hashed"),
                "User",
                Role.USER,
                false,
                null,
                null,
                LocalDateTime.now()
        );
    }
}
