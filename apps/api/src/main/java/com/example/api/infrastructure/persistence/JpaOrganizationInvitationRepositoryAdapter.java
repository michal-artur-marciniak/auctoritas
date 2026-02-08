package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.OrganizationInvitation;
import com.example.api.domain.organization.OrganizationInvitationId;
import com.example.api.domain.organization.OrganizationInvitationRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JPA adapter implementing the domain {@link OrganizationInvitationRepository} port.
 */
@Repository
public class JpaOrganizationInvitationRepositoryAdapter implements OrganizationInvitationRepository {

    private final OrganizationInvitationJpaRepository jpaRepository;

    public JpaOrganizationInvitationRepositoryAdapter(OrganizationInvitationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<OrganizationInvitation> findByToken(String token) {
        return jpaRepository.findByToken(token)
                .map(OrganizationInvitationDomainMapper::toDomain);
    }

    @Override
    public OrganizationInvitation save(OrganizationInvitation invitation) {
        final var entity = OrganizationInvitationDomainMapper.toEntity(invitation);
        jpaRepository.save(entity);
        return invitation;
    }

    @Override
    public void deleteById(OrganizationInvitationId id) {
        jpaRepository.deleteById(id.value());
    }

    @Override
    public void deleteExpired(LocalDateTime now) {
        jpaRepository.deleteByExpiresAtBefore(now);
    }
}
