package com.example.api.application.auth.sdk;

import com.example.api.application.auth.dto.UserDto;
import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;
import com.example.api.domain.user.*;
import com.example.api.domain.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for project-level isolation in SDK user operations.
 *
 * <p>US-005: As a platform operator, I want end users isolated per project
 * so data never leaks across tenants.</p>
 */
class ProjectLevelIsolationTest {

    private UserRepository userRepository;
    private GetCurrentSdkUserUseCase getCurrentSdkUserUseCase;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        getCurrentSdkUserUseCase = new GetCurrentSdkUserUseCase(userRepository);
    }

    @Test
    void getCurrentUserReturnsUserWhenInSameProject() {
        final var projectId = ProjectId.generate();
        final var environmentId = EnvironmentId.generate();
        final var userId = UserId.generate();
        final var email = new Email("user@example.com");
        final var password = Password.create("password123", new FakePasswordEncoder());

        final var user = User.registerEndUser(email, password, "Test User", projectId, environmentId);

        when(userRepository.findByIdAndProjectId(userId, projectId, environmentId))
                .thenReturn(Optional.of(user));

        final var result = getCurrentSdkUserUseCase.execute(userId.value(), projectId, environmentId);

        assertNotNull(result);
        assertEquals(user.getId().value(), result.id());
        assertEquals("user@example.com", result.email());
    }

    @Test
    void getCurrentUserThrowsNotFoundWhenUserInDifferentProject() {
        final var projectA = ProjectId.generate();
        final var projectB = ProjectId.generate();
        final var environmentId = EnvironmentId.generate();
        final var userId = UserId.generate();

        when(userRepository.findByIdAndProjectId(userId, projectB, environmentId))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                getCurrentSdkUserUseCase.execute(userId.value(), projectB, environmentId));
    }

    @Test
    void getCurrentUserThrowsNotFoundWhenUserInDifferentEnvironment() {
        final var projectId = ProjectId.generate();
        final var envProd = EnvironmentId.generate();
        final var envDev = EnvironmentId.generate();
        final var userId = UserId.generate();

        when(userRepository.findByIdAndProjectId(userId, projectId, envProd))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                getCurrentSdkUserUseCase.execute(userId.value(), projectId, envProd));
    }

    @Test
    void findByEmailAndProjectIdFiltersCorrectly() {
        final var projectA = ProjectId.generate();
        final var projectB = ProjectId.generate();
        final var environmentId = EnvironmentId.generate();
        final var email = new Email("user@example.com");
        final var password = Password.create("password123", new FakePasswordEncoder());

        final var userInProjectA = User.registerEndUser(email, password, "User A", projectA, environmentId);

        when(userRepository.findByEmailAndProjectId(email, projectA, environmentId))
                .thenReturn(Optional.of(userInProjectA));
        when(userRepository.findByEmailAndProjectId(email, projectB, environmentId))
                .thenReturn(Optional.empty());

        final var foundInA = userRepository.findByEmailAndProjectId(email, projectA, environmentId);
        final var foundInB = userRepository.findByEmailAndProjectId(email, projectB, environmentId);

        assertTrue(foundInA.isPresent());
        assertTrue(foundInB.isEmpty());
        assertEquals("User A", foundInA.get().getName());
    }

    @Test
    void userBelongsToChecksBothProjectAndEnvironment() {
        final var projectId = ProjectId.generate();
        final var environmentId = EnvironmentId.generate();
        final var otherProject = ProjectId.generate();
        final var otherEnvironment = EnvironmentId.generate();
        final var email = new Email("user@example.com");
        final var password = Password.create("password123", new FakePasswordEncoder());

        final var user = User.registerEndUser(email, password, "Test User", projectId, environmentId);

        assertTrue(user.belongsTo(projectId, environmentId));
        assertFalse(user.belongsTo(otherProject, environmentId));
        assertFalse(user.belongsTo(projectId, otherEnvironment));
        assertFalse(user.belongsTo(otherProject, otherEnvironment));
    }

    @Test
    void mixedProjectDataIsolation() {
        final var projectA = ProjectId.generate();
        final var projectB = ProjectId.generate();
        final var envA = EnvironmentId.generate();
        final var envB = EnvironmentId.generate();
        final var userId = UserId.generate();
        final var password = Password.create("password123", new FakePasswordEncoder());

        final var userA = User.registerEndUser(
                new Email("user@project-a.com"), password, "User A", projectA, envA);
        final var userB = User.registerEndUser(
                new Email("user@project-b.com"), password, "User B", projectB, envB);

        when(userRepository.findByIdAndProjectId(userId, projectA, envA))
                .thenReturn(Optional.of(userA));
        when(userRepository.findByIdAndProjectId(userId, projectB, envB))
                .thenReturn(Optional.of(userB));

        final var resultA = getCurrentSdkUserUseCase.execute(userId.value(), projectA, envA);
        final var resultB = getCurrentSdkUserUseCase.execute(userId.value(), projectB, envB);

        assertEquals("User A", resultA.name());
        assertEquals("User B", resultB.name());
    }

    /**
     * Fake password encoder for testing.
     */
    private static class FakePasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(String rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return encodedPassword.equals("encoded:" + rawPassword);
        }
    }
}
