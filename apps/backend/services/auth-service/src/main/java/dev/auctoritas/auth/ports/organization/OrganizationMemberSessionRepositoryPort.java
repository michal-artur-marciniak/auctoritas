package dev.auctoritas.auth.ports.organization;

import dev.auctoritas.auth.domain.model.organization.OrganizationMemberSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for OrganizationMemberSession persistence operations.
 */
public interface OrganizationMemberSessionRepositoryPort {

  Optional<OrganizationMemberSession> findByMemberId(UUID memberId);

  List<OrganizationMemberSession> findByExpiresAtBefore(Instant now);

  void deleteByExpiresAtBefore(Instant now);

  OrganizationMemberSession save(OrganizationMemberSession session);

  Optional<OrganizationMemberSession> findById(UUID id);

  void deleteById(UUID id);
}
