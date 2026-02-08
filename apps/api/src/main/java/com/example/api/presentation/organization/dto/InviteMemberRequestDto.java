package com.example.api.presentation.organization.dto;

import com.example.api.application.organization.dto.InviteMemberRequest;
import com.example.api.domain.organization.OrganizationMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * REST request DTO for inviting an organization member.
 */
public record InviteMemberRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Role is required")
        OrganizationMemberRole role
) {
    public InviteMemberRequest toRequest(String organizationId, String invitedByMemberId) {
        return new InviteMemberRequest(organizationId, invitedByMemberId, email, role);
    }
}
