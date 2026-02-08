package com.example.api.application.organization.dto;

import com.example.api.domain.organization.OrganizationInvitation;

import java.time.LocalDateTime;

/**
 * Response DTO for organization invitations.
 */
public record InvitationResponse(
        String id,
        String organizationId,
        String email,
        String role,
        String token,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static InvitationResponse from(OrganizationInvitation invitation) {
        return new InvitationResponse(
                invitation.getId().value(),
                invitation.getOrganizationId().value(),
                invitation.getEmail().value(),
                invitation.getRole().name(),
                invitation.getToken(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}
