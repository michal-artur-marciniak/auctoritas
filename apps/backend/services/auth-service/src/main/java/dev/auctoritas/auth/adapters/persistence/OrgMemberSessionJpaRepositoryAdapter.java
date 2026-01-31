package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.domain.model.organization.OrgMemberSession;
import dev.auctoritas.auth.ports.organization.OrgMemberSessionRepositoryPort;
import dev.auctoritas.auth.repository.OrgMemberSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OrgMemberSessionRepository} via {@link OrgMemberSessionRepositoryPort}.
 */
@Component
public class OrgMemberSessionJpaRepositoryAdapter implements OrgMemberSessionRepositoryPort {

  private final OrgMemberSessionRepository orgMemberSessionRepository;

  public OrgMemberSessionJpaRepositoryAdapter(OrgMemberSessionRepository orgMemberSessionRepository) {
    this.orgMemberSessionRepository = orgMemberSessionRepository;
  }

  @Override
  public Optional<OrgMemberSession> findByMemberId(UUID memberId) {
    return orgMemberSessionRepository.findByMemberId(memberId);
  }

  @Override
  public List<OrgMemberSession> findByExpiresAtBefore(Instant now) {
    return orgMemberSessionRepository.findByExpiresAtBefore(now);
  }

  @Override
  public void deleteByExpiresAtBefore(Instant now) {
    orgMemberSessionRepository.deleteByExpiresAtBefore(now);
  }

  @Override
  public OrgMemberSession save(OrgMemberSession session) {
    return orgMemberSessionRepository.save(session);
  }

  @Override
  public Optional<OrgMemberSession> findById(UUID id) {
    return orgMemberSessionRepository.findById(id);
  }

  @Override
  public void deleteById(UUID id) {
    orgMemberSessionRepository.deleteById(id);
  }
}
