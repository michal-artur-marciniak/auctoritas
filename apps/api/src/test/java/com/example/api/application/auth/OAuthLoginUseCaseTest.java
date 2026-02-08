package com.example.api.application.auth;

import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import com.example.api.domain.user.Role;
import com.example.api.domain.user.User;
import com.example.api.domain.user.UserId;
import com.example.api.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthLoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private AuthSessionService authSessionService;

    @InjectMocks
    private OAuthLoginUseCase useCase;

    @Test
    void returnsTokensForExistingUser() {
        final var user = existingUser();
        when(userRepository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        final var response = useCase.execute("user@example.com", "User");

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(user.getId().value(), response.user().id());
        verify(authSessionService).createSession(user);
    }

    @Test
    void createsUserWhenMissing() {
        final var user = existingUser();
        when(userRepository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        final var response = useCase.execute("user@example.com", "User");

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        verify(userRepository).save(any(User.class));
        verify(authSessionService).createSession(user);
    }

    private User existingUser() {
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
