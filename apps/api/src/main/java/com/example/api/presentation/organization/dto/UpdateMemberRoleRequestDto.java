package com.example.api.presentation.organization.dto;

import com.example.api.application.organization.dto.UpdateMemberRoleRequest;
import com.example.api.domain.organization.OrganizationMemberRole;
import jakarta.validation.constraints.NotNull;

/**
 * REST request DTO for updating a member role.
 */
public record UpdateMemberRoleRequestDto(
        @NotNull(message = "Role is required")
        OrganizationMemberRole role
) {
    public UpdateMemberRoleRequest toRequest(String organizationId, String actorMemberId, String memberId) {
        return new UpdateMemberRoleRequest(organizationId, actorMemberId, memberId, role);
    }
}
