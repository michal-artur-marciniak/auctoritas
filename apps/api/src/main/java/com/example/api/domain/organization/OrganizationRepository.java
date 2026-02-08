package com.example.api.domain.organization;

import java.util.Optional;

/**
 * Repository port for organization persistence.
 */
public interface OrganizationRepository {

    Optional<Organization> findById(OrganizationId id);

    Optional<Organization> findBySlug(String slug);

    Organization save(Organization organization);
}
