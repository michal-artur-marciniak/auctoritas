package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Environment entities.
 */
@Repository
public interface EnvironmentJpaRepository extends JpaRepository<EnvironmentJpaEntity, String> {

    List<EnvironmentJpaEntity> findByProjectId(String projectId);

    Optional<EnvironmentJpaEntity> findByProjectIdAndEnvironmentType(String projectId, String environmentType);
}
