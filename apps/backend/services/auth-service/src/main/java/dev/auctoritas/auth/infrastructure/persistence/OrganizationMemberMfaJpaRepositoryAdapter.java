package dev.auctoritas.auth.infrastructure.persistence;

import dev.auctoritas.auth.domain.organization.OrganizationMemberMfa;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfaRepositoryPort;
import dev.auctoritas.auth.infrastructure.persistence.repository.OrganizationMemberMfaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OrganizationMemberMfaRepository} via {@link OrganizationMemberMfaRepositoryPort}.
 */
@Component
public class OrganizationMemberMfaJpaRepositoryAdapter implements OrganizationMemberMfaRepositoryPort {

  private final OrganizationMemberMfaRepository orgMemberMfaRepository;

  public OrganizationMemberMfaJpaRepositoryAdapter(OrganizationMemberMfaRepository orgMemberMfaRepository) {
    this.orgMemberMfaRepository = orgMemberMfaRepository;
  }

  @Override
  public Optional<OrganizationMemberMfa> findByMemberId(UUID memberId) {
    return orgMemberMfaRepository.findByMemberId(memberId);
  }

  @Override
  public OrganizationMemberMfa save(OrganizationMemberMfa mfa) {
    return orgMemberMfaRepository.save(mfa);
  }
}
