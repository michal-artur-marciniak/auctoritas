package dev.auctoritas.auth.ports.organization;

import dev.auctoritas.auth.entity.organization.OrganizationInvitation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for OrganizationInvitation persistence operations.
 */
public interface OrganizationInvitationRepositoryPort {

  Optional<OrganizationInvitation> findByToken(String token);

  Optional<OrganizationInvitation> findByEmailAndOrganizationId(String email, UUID organizationId);

  List<OrganizationInvitation> findByExpiresAtBefore(Instant now);

  OrganizationInvitation save(OrganizationInvitation invitation);
}
