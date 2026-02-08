package com.example.api.domain.user;

import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;

import java.util.Optional;

/**
 * Repository port for User aggregate persistence.
 *
 * <p>Defined in the domain layer; implemented by infrastructure adapters.</p>
 */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

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
}
