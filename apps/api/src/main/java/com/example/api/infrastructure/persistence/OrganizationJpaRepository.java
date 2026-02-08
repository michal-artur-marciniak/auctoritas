package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for organizations.
 */
public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, String> {

    Optional<OrganizationJpaEntity> findBySlug(String slug);
}
