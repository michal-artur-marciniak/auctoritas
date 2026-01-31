package dev.auctoritas.auth.ports.organization;

import dev.auctoritas.auth.domain.model.organization.OrgMemberSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for OrgMemberSession persistence operations.
 */
public interface OrgMemberSessionRepositoryPort {

  Optional<OrgMemberSession> findByMemberId(UUID memberId);

  List<OrgMemberSession> findByExpiresAtBefore(Instant now);

  void deleteByExpiresAtBefore(Instant now);

  OrgMemberSession save(OrgMemberSession session);

  Optional<OrgMemberSession> findById(UUID id);

  void deleteById(UUID id);
}
