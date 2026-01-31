package dev.auctoritas.auth.infrastructure.persistence;

import dev.auctoritas.auth.domain.organization.OrganizationMemberSession;
import dev.auctoritas.auth.domain.organization.OrganizationMemberSessionRepositoryPort;
import dev.auctoritas.auth.infrastructure.persistence.repository.OrganizationMemberSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OrganizationMemberSessionRepository} via {@link OrganizationMemberSessionRepositoryPort}.
 */
@Component
public class OrganizationMemberSessionJpaRepositoryAdapter implements OrganizationMemberSessionRepositoryPort {

  private final OrganizationMemberSessionRepository orgMemberSessionRepository;

  public OrganizationMemberSessionJpaRepositoryAdapter(OrganizationMemberSessionRepository orgMemberSessionRepository) {
    this.orgMemberSessionRepository = orgMemberSessionRepository;
  }

  @Override
  public Optional<OrganizationMemberSession> findByMemberId(UUID memberId) {
    return orgMemberSessionRepository.findByMemberId(memberId);
  }

  @Override
  public List<OrganizationMemberSession> findByExpiresAtBefore(Instant now) {
    return orgMemberSessionRepository.findByExpiresAtBefore(now);
  }

  @Override
  public void deleteByExpiresAtBefore(Instant now) {
    orgMemberSessionRepository.deleteByExpiresAtBefore(now);
  }

  @Override
  public OrganizationMemberSession save(OrganizationMemberSession session) {
    return orgMemberSessionRepository.save(session);
  }

  @Override
  public Optional<OrganizationMemberSession> findById(UUID id) {
    return orgMemberSessionRepository.findById(id);
  }

  @Override
  public void deleteById(UUID id) {
    orgMemberSessionRepository.deleteById(id);
  }
}
