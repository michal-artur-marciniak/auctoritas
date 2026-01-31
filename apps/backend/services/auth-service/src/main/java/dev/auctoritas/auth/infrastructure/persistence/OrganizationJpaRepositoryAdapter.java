package dev.auctoritas.auth.infrastructure.persistence;

import dev.auctoritas.auth.domain.organization.OrganizationStatus;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.organization.OrganizationRepositoryPort;
import dev.auctoritas.auth.infrastructure.persistence.repository.OrganizationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OrganizationRepository} via {@link OrganizationRepositoryPort}.
 */
@Component
public class OrganizationJpaRepositoryAdapter implements OrganizationRepositoryPort {

  private final OrganizationRepository organizationRepository;

  public OrganizationJpaRepositoryAdapter(OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
  }

  @Override
  public Optional<Organization> findBySlug(String slug) {
    return organizationRepository.findBySlug(slug);
  }

  @Override
  public boolean existsBySlug(String slug) {
    return organizationRepository.existsBySlug(slug);
  }

  @Override
  public List<Organization> findByStatus(OrganizationStatus status) {
    return organizationRepository.findByStatus(status);
  }

  @Override
  public Optional<Organization> findById(UUID id) {
    return organizationRepository.findById(id);
  }

  @Override
  public Organization save(Organization organization) {
    return organizationRepository.save(organization);
  }
}
