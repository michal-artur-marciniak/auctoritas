package com.example.api.domain.organization;

import com.example.api.domain.user.Email;

import java.util.Optional;

/**
 * Repository port for organization member persistence.
 */
public interface OrganizationMemberRepository {

    Optional<OrganizationMember> findById(OrganizationMemberId id);

    Optional<OrganizationMember> findByEmailAndOrganizationId(Email email, OrganizationId organizationId);

    OrganizationMember save(OrganizationMember member);
}
