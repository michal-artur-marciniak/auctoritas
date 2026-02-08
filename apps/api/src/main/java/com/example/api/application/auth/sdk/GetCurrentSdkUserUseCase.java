package com.example.api.application.auth.sdk;

import com.example.api.application.auth.dto.UserDto;
import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;
import com.example.api.domain.user.UserId;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.UserNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Use case for retrieving the current SDK end user scoped to project and environment.
 *
 * <p>This ensures project-level isolation - users from one project cannot access
 * data from another project.</p>
 */
@Component
public class GetCurrentSdkUserUseCase {

    private final UserRepository userRepository;

    public GetCurrentSdkUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the user data scoped to the given project and environment.
     *
     * <p>If the user ID does not exist in the given project/environment scope,
     * returns not found to prevent cross-project data leakage.</p>
     *
     * @param userId the user's ID
     * @param projectId the project context from API key
     * @param environmentId the environment context from API key
     * @return user DTO
     * @throws UserNotFoundException if user not found in this project/environment scope
     */
    public UserDto execute(String userId, ProjectId projectId, EnvironmentId environmentId) {
        Objects.requireNonNull(userId, "User ID required");
        Objects.requireNonNull(projectId, "Project ID required");
        Objects.requireNonNull(environmentId, "Environment ID required");

        final var user = userRepository.findByIdAndProjectId(
                        UserId.of(userId), projectId, environmentId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserDto.fromDomain(user);
    }
}
