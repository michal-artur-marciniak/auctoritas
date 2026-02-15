package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.OrganizationMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for organization members.
 */
public interface OrganizationMemberJpaRepository extends JpaRepository<OrganizationMemberJpaEntity, String> {

    Optional<OrganizationMemberJpaEntity> findByEmailAndOrganizationId(String email, String organizationId);

    long countByOrganizationIdAndRole(String organizationId, OrganizationMemberRole role);

    List<OrganizationMemberJpaEntity> findAllByOrganizationId(String organizationId);
}
