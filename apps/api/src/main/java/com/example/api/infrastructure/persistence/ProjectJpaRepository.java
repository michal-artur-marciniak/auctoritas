package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Project entities.
 */
@Repository
public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, String> {

    Optional<ProjectJpaEntity> findBySlugAndOrganizationId(String slug, String organizationId);

    List<ProjectJpaEntity> findByOrganizationId(String organizationId);
}
