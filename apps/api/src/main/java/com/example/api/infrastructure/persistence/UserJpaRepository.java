package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserJpaEntity}.
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByEmailAndProjectIdAndEnvironmentId(
            String email, String projectId, String environmentId);

    Optional<UserJpaEntity> findByIdAndProjectIdAndEnvironmentId(
            String id, String projectId, String environmentId);

    /**
     * Finds users by email partial match (case-insensitive).
     */
    List<UserJpaEntity> findByEmailContainingIgnoreCase(String email);

    /**
     * Finds users by project ID.
     */
    List<UserJpaEntity> findByProjectId(String projectId);
}
