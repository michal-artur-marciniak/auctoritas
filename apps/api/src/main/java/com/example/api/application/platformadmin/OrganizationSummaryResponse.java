package com.example.api.application.platformadmin;

import com.example.api.domain.organization.Organization;

import java.time.LocalDateTime;

/**
 * Response DTO for organization summary (list view).
 */
public record OrganizationSummaryResponse(
        String id,
        String name,
        String slug,
        String status,
        long memberCount,
        LocalDateTime createdAt
) {
    public static OrganizationSummaryResponse from(Organization organization, long memberCount) {
        return new OrganizationSummaryResponse(
                organization.getId().value(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus().name(),
                memberCount,
                organization.getCreatedAt()
        );
    }
}
