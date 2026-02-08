package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.Organization;
import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter implementing the domain {@link OrganizationRepository} port.
 */
@Repository
public class JpaOrganizationRepositoryAdapter implements OrganizationRepository {

    private final OrganizationJpaRepository jpaRepository;

    public JpaOrganizationRepositoryAdapter(OrganizationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Organization> findById(OrganizationId id) {
        return jpaRepository.findById(id.value())
                .map(OrganizationDomainMapper::toDomain);
    }

    @Override
    public Optional<Organization> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug)
                .map(OrganizationDomainMapper::toDomain);
    }

    @Override
    public Organization save(Organization organization) {
        final var entity = OrganizationDomainMapper.toEntity(organization);
        jpaRepository.save(entity);
        return organization;
    }
}
