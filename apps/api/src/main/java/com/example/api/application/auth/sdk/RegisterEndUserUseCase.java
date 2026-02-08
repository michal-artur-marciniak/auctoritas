package com.example.api.application.auth.sdk;

import com.example.api.application.auth.TokenProvider;
import com.example.api.application.auth.dto.UserDto;
import com.example.api.application.auth.sdk.dto.SdkAuthResponse;
import com.example.api.application.auth.sdk.dto.SdkRegisterRequest;
import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import com.example.api.domain.user.User;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.EmailAlreadyExistsException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Use case for registering an SDK end user scoped to a project and environment.
 */
@Component
public class RegisterEndUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public RegisterEndUserUseCase(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Registers a new end user scoped to the given project and environment.
     *
     * @param request the registration data
     * @param projectId the project context from API key
     * @param environmentId the environment context from API key
     * @return authentication response with SDK JWT
     * @throws EmailAlreadyExistsException if email already exists in this project/environment
     */
    @Transactional
    public SdkAuthResponse execute(SdkRegisterRequest request, ProjectId projectId, EnvironmentId environmentId) {
        Objects.requireNonNull(projectId, "Project ID required");
        Objects.requireNonNull(environmentId, "Environment ID required");

        final var email = new Email(request.email());

        // Check if email exists in this project/environment scope
        if (userRepository.findByEmailAndProjectId(email, projectId, environmentId).isPresent()) {
            throw new EmailAlreadyExistsException(request.email());
        }

        final var user = User.registerEndUser(
                email,
                Password.create(request.password(), passwordEncoder),
                request.name(),
                projectId,
                environmentId
        );

        userRepository.save(user);

        final var accessToken = tokenProvider.generateAccessToken(user);
        final var refreshToken = tokenProvider.generateRefreshToken(user);

        return new SdkAuthResponse(accessToken, refreshToken, UserDto.fromDomain(user));
    }
}
