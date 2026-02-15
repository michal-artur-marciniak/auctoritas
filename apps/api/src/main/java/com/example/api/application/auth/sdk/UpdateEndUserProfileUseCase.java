package com.example.api.application.auth.sdk;

import com.example.api.application.auth.dto.UserDto;
import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.UserId;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.EmailAlreadyExistsException;
import com.example.api.domain.user.exception.UserNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Use case for updating the current end user's profile.
 *
 * <p>Ensures email uniqueness is enforced within the project and environment scope.
 * This maintains isolation between different projects using the platform.</p>
 */
@Component
public class UpdateEndUserProfileUseCase {

    private final UserRepository userRepository;

    public UpdateEndUserProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Updates the end user's profile with project-scoped email uniqueness validation.
     *
     * @param userId the user's ID
     * @param projectId the project context from API key
     * @param environmentId the environment context from API key
     * @param email the new email address
     * @param name the new name
     * @return updated user DTO
     * @throws UserNotFoundException if user not found in this project/environment scope
     * @throws EmailAlreadyExistsException if email is already used by another user in this scope
     */
    public UserDto execute(String userId, ProjectId projectId, EnvironmentId environmentId,
                           String email, String name) {
        Objects.requireNonNull(userId, "User ID required");
        Objects.requireNonNull(projectId, "Project ID required");
        Objects.requireNonNull(environmentId, "Environment ID required");
        Objects.requireNonNull(email, "Email required");
        Objects.requireNonNull(name, "Name required");

        final var user = userRepository.findByIdAndProjectId(
                        UserId.of(userId), projectId, environmentId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        final var newEmail = new Email(email);

        // Check email uniqueness within project scope
        if (!newEmail.value().equals(user.getEmail().value())) {
            userRepository.findByEmailAndProjectId(newEmail, projectId, environmentId)
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(user.getId())) {
                            throw new EmailAlreadyExistsException(email);
                        }
                    });
        }

        user.changeEmail(newEmail);
        user.changeName(name);
        userRepository.save(user);

        return UserDto.fromDomain(user);
    }
}
