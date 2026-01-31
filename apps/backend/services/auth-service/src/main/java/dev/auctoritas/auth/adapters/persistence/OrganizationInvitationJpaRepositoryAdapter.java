package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.domain.model.organization.OrganizationInvitation;
import dev.auctoritas.auth.ports.organization.OrganizationInvitationRepositoryPort;
import dev.auctoritas.auth.repository.OrganizationInvitationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OrganizationInvitationRepository} via {@link OrganizationInvitationRepositoryPort}.
 */
@Component
public class OrganizationInvitationJpaRepositoryAdapter implements OrganizationInvitationRepositoryPort {

  private final OrganizationInvitationRepository organizationInvitationRepository;

  public OrganizationInvitationJpaRepositoryAdapter(OrganizationInvitationRepository organizationInvitationRepository) {
    this.organizationInvitationRepository = organizationInvitationRepository;
  }

  @Override
  public Optional<OrganizationInvitation> findByToken(String token) {
    return organizationInvitationRepository.findByToken(token);
  }

  @Override
  public Optional<OrganizationInvitation> findByEmailAndOrganizationId(String email, UUID organizationId) {
    return organizationInvitationRepository.findByEmailAndOrganizationId(email, organizationId);
  }

  @Override
  public List<OrganizationInvitation> findByExpiresAtBefore(Instant now) {
    return organizationInvitationRepository.findByExpiresAtBefore(now);
  }

  @Override
  public OrganizationInvitation save(OrganizationInvitation invitation) {
    return organizationInvitationRepository.save(invitation);
  }
}
