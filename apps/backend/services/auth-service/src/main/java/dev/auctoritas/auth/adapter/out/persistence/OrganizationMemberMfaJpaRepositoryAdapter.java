package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.adapter.out.persistence.repository.OrganizationMemberMfaRepository;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfa;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfaRepositoryPort;
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
  public Optional<OrganizationMemberMfa> findByMemberIdForUpdate(UUID memberId) {
    return orgMemberMfaRepository.findByMemberIdForUpdate(memberId);
  }

  @Override
  public OrganizationMemberMfa save(OrganizationMemberMfa mfa) {
    return orgMemberMfaRepository.save(mfa);
  }

  @Override
  public void delete(OrganizationMemberMfa mfa) {
    orgMemberMfaRepository.delete(mfa);
  }

  @Override
  public boolean isEnabledByMemberId(UUID memberId) {
    return orgMemberMfaRepository.existsByMemberIdAndEnabledTrue(memberId);
  }
}
