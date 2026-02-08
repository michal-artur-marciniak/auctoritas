package com.example.api.application.organization.dto;

/**
 * Application request for creating an organization.
 */
public record CreateOrganizationRequest(
        String name,
        String slug,
        String ownerEmail,
        String ownerPassword,
        String ownerName
) {
}
