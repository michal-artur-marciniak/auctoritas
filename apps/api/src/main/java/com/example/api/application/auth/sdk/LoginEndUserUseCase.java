package com.example.api.application.auth.sdk;

import com.example.api.application.auth.TokenProvider;
import com.example.api.application.auth.dto.UserDto;
import com.example.api.application.auth.sdk.dto.SdkAuthResponse;
import com.example.api.application.auth.sdk.dto.SdkLoginRequest;
import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.PasswordEncoder;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.InvalidCredentialsException;
import com.example.api.domain.user.exception.UserBannedException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Use case for logging in an SDK end user scoped to a project and environment.
 */
@Component
public class LoginEndUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public LoginEndUserUseCase(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Authenticates an end user within the given project and environment scope.
     *
     * @param request the login credentials
     * @param projectId the project context from API key
     * @param environmentId the environment context from API key
     * @return authentication response with SDK JWT
     * @throws InvalidCredentialsException if credentials are invalid or user not found
     */
    public SdkAuthResponse execute(SdkLoginRequest request, ProjectId projectId, EnvironmentId environmentId) {
        Objects.requireNonNull(projectId, "Project ID required");
        Objects.requireNonNull(environmentId, "Environment ID required");

        final var email = new Email(request.email());
        final var user = userRepository.findByEmailAndProjectId(email, projectId, environmentId)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.canLogin()) {
            throw new UserBannedException();
        }

        if (!user.getPassword().matches(request.password(), passwordEncoder)) {
            throw new InvalidCredentialsException();
        }

        final var accessToken = tokenProvider.generateAccessToken(user);
        final var refreshToken = tokenProvider.generateRefreshToken(user);

        return new SdkAuthResponse(accessToken, refreshToken, UserDto.fromDomain(user));
    }
}
