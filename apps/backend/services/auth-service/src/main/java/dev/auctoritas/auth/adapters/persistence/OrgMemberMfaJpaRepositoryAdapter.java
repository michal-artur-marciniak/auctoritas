package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.entity.organization.OrgMemberMfa;
import dev.auctoritas.auth.ports.organization.OrgMemberMfaRepositoryPort;
import dev.auctoritas.auth.repository.OrgMemberMfaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OrgMemberMfaRepository} via {@link OrgMemberMfaRepositoryPort}.
 */
@Component
public class OrgMemberMfaJpaRepositoryAdapter implements OrgMemberMfaRepositoryPort {

  private final OrgMemberMfaRepository orgMemberMfaRepository;

  public OrgMemberMfaJpaRepositoryAdapter(OrgMemberMfaRepository orgMemberMfaRepository) {
    this.orgMemberMfaRepository = orgMemberMfaRepository;
  }

  @Override
  public Optional<OrgMemberMfa> findByMemberId(UUID memberId) {
    return orgMemberMfaRepository.findByMemberId(memberId);
  }

  @Override
  public OrgMemberMfa save(OrgMemberMfa mfa) {
    return orgMemberMfaRepository.save(mfa);
  }
}
