package dev.auctoritas.auth.domain.organization;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for OrganizationMemberMfa persistence operations.
 */
public interface OrganizationMemberMfaRepositoryPort {

  Optional<OrganizationMemberMfa> findByMemberId(UUID memberId);

  OrganizationMemberMfa save(OrganizationMemberMfa mfa);
}
