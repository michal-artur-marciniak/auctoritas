package dev.auctoritas.auth.ports.organization;

import dev.auctoritas.auth.domain.organization.OrgMemberStatus;
import dev.auctoritas.auth.domain.model.organization.OrganizationMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for OrganizationMember persistence operations.
 */
public interface OrganizationMemberRepositoryPort {

  Optional<OrganizationMember> findByEmailAndOrganizationId(String email, UUID organizationId);

  boolean existsByEmailAndOrganizationId(String email, UUID organizationId);

  List<OrganizationMember> findByOrganizationId(UUID organizationId);

  List<OrganizationMember> findByStatus(OrgMemberStatus status);

  Optional<OrganizationMember> findByIdWithOrganization(UUID id);

  Optional<OrganizationMember> findById(UUID id);

  OrganizationMember save(OrganizationMember member);
}
