package com.example.api.application.organization.dto;

/**
 * Authentication response for org member login.
 */
public record OrgAuthResponse(
        String accessToken,
        OrganizationMemberResponse member
) {
}
