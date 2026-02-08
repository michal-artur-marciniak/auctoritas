package com.example.api.application.auth;

import com.example.api.application.auth.dto.RefreshTokenRequest;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.Role;
import com.example.api.domain.user.User;
import com.example.api.domain.user.UserId;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.InvalidCredentialsException;
import com.example.api.domain.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private AuthSessionService authSessionService;

    @InjectMocks
    private RefreshTokenUseCase useCase;

    @Test
    void rejectsInvalidRefreshToken() {
        final var request = new RefreshTokenRequest("bad-token");
        when(tokenProvider.validateRefreshToken("bad-token")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(request));

        verify(tokenProvider).validateRefreshToken("bad-token");
        verify(tokenProvider, never()).getUserIdFromToken(anyString());
        verifyNoInteractions(userRepository);
    }

    @Test
    void refreshesAccessToken() {
        final var user = sampleUser();
        final var request = new RefreshTokenRequest("refresh-token");

        when(tokenProvider.validateRefreshToken("refresh-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("refresh-token")).thenReturn(user.getId().value());
        when(userRepository.findById(UserId.of(user.getId().value()))).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");

        final var response = useCase.execute(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(user.getId().value(), response.user().id());
        verify(authSessionService).extendSession(user);
    }

    @Test
    void throwsWhenUserMissing() {
        final var request = new RefreshTokenRequest("refresh-token");

        when(tokenProvider.validateRefreshToken("refresh-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("refresh-token")).thenReturn("missing-id");
        when(userRepository.findById(UserId.of("missing-id"))).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(request));
    }

    private User sampleUser() {
        return new User(
                UserId.of("user-id"),
                new Email("user@example.com"),
                Password.fromHash("hashed"),
                "Test User",
                Role.USER,
                false,
                null,
                null,
                LocalDateTime.now()
        );
    }
}
