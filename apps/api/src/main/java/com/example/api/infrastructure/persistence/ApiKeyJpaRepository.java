package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for ApiKey entities.
 */
@Repository
public interface ApiKeyJpaRepository extends JpaRepository<ApiKeyJpaEntity, String> {

    Optional<ApiKeyJpaEntity> findByKeyHash(String keyHash);

    List<ApiKeyJpaEntity> findByProjectId(String projectId);
}
