package dev.auctoritas.auth.ports.organization;

import dev.auctoritas.auth.domain.model.organization.OrganizationMemberMfa;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for OrganizationMemberMfa persistence operations.
 */
public interface OrganizationMemberMfaRepositoryPort {

  Optional<OrganizationMemberMfa> findByMemberId(UUID memberId);

  OrganizationMemberMfa save(OrganizationMemberMfa mfa);
}
