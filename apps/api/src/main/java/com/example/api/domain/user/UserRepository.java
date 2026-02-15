package com.example.api.domain.user;

import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for User aggregate persistence.
 *
 * <p>Defined in the domain layer; implemented by infrastructure adapters.</p>
 */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    /**
     * Finds a user by email scoped to a specific project and environment.
     */
    Optional<User> findByEmailAndProjectId(Email email, ProjectId projectId, EnvironmentId environmentId);

    /**
     * Finds a user by ID scoped to a specific project and environment.
     */
    Optional<User> findByIdAndProjectId(UserId id, ProjectId projectId, EnvironmentId environmentId);

    boolean existsByEmail(Email email);

    User save(User user);

    void delete(UserId id);

    /**
     * Finds all users across all projects (for platform admin access).
     */
    List<User> findAll();

    /**
     * Finds users by email partial match (for platform admin search).
     */
    List<User> findByEmailContainingIgnoreCase(String email);

    /**
     * Finds users by project ID (for platform admin filtering).
     */
    List<User> findByProjectId(ProjectId projectId);
}
