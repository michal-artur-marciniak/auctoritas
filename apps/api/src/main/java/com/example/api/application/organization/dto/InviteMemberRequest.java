package com.example.api.application.organization.dto;

import com.example.api.domain.organization.OrganizationMemberRole;

/**
 * Application request for inviting an organization member.
 */
public record InviteMemberRequest(
        String organizationId,
        String invitedByMemberId,
        String email,
        OrganizationMemberRole role
) {
}
