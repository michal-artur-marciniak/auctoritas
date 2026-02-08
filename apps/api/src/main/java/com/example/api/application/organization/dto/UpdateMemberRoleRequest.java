package com.example.api.application.organization.dto;

import com.example.api.domain.organization.OrganizationMemberRole;

/**
 * Application request for updating a member role.
 */
public record UpdateMemberRoleRequest(
        String organizationId,
        String actorMemberId,
        String memberId,
        OrganizationMemberRole role
) {
}
