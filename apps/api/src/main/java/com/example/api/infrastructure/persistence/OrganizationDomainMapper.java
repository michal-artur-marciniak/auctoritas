package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.Organization;
import com.example.api.domain.organization.OrganizationId;

/**
 * Maps between domain {@link Organization} and JPA {@link OrganizationJpaEntity}.
 */
final class OrganizationDomainMapper {

    private OrganizationDomainMapper() {
        // Utility class
    }

    static Organization toDomain(OrganizationJpaEntity entity) {
        return new Organization(
                OrganizationId.of(entity.getId()),
                entity.getName(),
                entity.getSlug(),
                entity.getStatus(),
                entity.getStripeCustomerId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    static OrganizationJpaEntity toEntity(Organization organization) {
        return new OrganizationJpaEntity(
                organization.getId().value(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus(),
                organization.getStripeCustomerId(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }
}
