package com.example.api.application.organization.dto;

/**
 * Application request for accepting an organization invitation.
 */
public record AcceptInvitationRequest(
        String organizationId,
        String token,
        String name,
        String password
) {
}
