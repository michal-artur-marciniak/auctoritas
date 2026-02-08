package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for organization members.
 */
public interface OrganizationMemberJpaRepository extends JpaRepository<OrganizationMemberJpaEntity, String> {

    Optional<OrganizationMemberJpaEntity> findByEmailAndOrganizationId(String email, String organizationId);
}
