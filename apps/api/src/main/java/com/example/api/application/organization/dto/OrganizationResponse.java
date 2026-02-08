package com.example.api.application.organization.dto;

import com.example.api.domain.organization.Organization;
import com.example.api.domain.organization.OrganizationMember;

import java.time.LocalDateTime;

/**
 * Response payload for organization creation.
 */
public record OrganizationResponse(
        String organizationId,
        String name,
        String slug,
        String status,
        String ownerId,
        String ownerEmail,
        String ownerName,
        LocalDateTime createdAt
) {
    public static OrganizationResponse from(Organization organization, OrganizationMember owner) {
        return new OrganizationResponse(
                organization.getId().value(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus().name(),
                owner.getId().value(),
                owner.getEmail().value(),
                owner.getName(),
                organization.getCreatedAt()
        );
    }
}
