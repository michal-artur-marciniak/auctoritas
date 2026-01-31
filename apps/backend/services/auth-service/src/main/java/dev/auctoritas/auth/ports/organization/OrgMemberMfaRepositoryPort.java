package dev.auctoritas.auth.ports.organization;

import dev.auctoritas.auth.entity.organization.OrgMemberMfa;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for OrgMemberMfa persistence operations.
 */
public interface OrgMemberMfaRepositoryPort {

  Optional<OrgMemberMfa> findByMemberId(UUID memberId);

  OrgMemberMfa save(OrgMemberMfa mfa);
}
