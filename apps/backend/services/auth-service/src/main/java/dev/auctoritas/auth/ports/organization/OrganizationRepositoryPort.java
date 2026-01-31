package dev.auctoritas.auth.ports.organization;

import dev.auctoritas.auth.domain.organization.OrganizationStatus;
import dev.auctoritas.auth.domain.model.organization.Organization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for Organization persistence operations.
 */
public interface OrganizationRepositoryPort {

  Optional<Organization> findBySlug(String slug);

  boolean existsBySlug(String slug);

  List<Organization> findByStatus(OrganizationStatus status);

  Optional<Organization> findById(UUID id);

  Organization save(Organization organization);
}
