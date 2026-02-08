package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA repository for organization invitations.
 */
public interface OrganizationInvitationJpaRepository
        extends JpaRepository<OrganizationInvitationJpaEntity, String> {

    Optional<OrganizationInvitationJpaEntity> findByToken(String token);

    long deleteByExpiresAtBefore(LocalDateTime now);
}
