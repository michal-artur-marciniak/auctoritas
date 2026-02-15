package com.example.api.domain.organization;

import com.example.api.domain.user.Email;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for organization member persistence.
 */
public interface OrganizationMemberRepository {

    Optional<OrganizationMember> findById(OrganizationMemberId id);

    Optional<OrganizationMember> findByEmailAndOrganizationId(Email email, OrganizationId organizationId);

    long countByOrganizationIdAndRole(OrganizationId organizationId, OrganizationMemberRole role);

    List<OrganizationMember> findByOrganizationId(OrganizationId organizationId);

    OrganizationMember save(OrganizationMember member);

    void delete(OrganizationMemberId id);
}
