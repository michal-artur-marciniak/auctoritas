package com.example.api.application.platformadmin;

import com.example.api.application.organization.dto.OrganizationMemberResponse;
import com.example.api.application.project.dto.ProjectResponse;
import com.example.api.domain.organization.Organization;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for organization details (full view with projects and members).
 */
public record OrganizationDetailsResponse(
        String id,
        String name,
        String slug,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrganizationMemberResponse> members,
        List<ProjectResponse> projects
) {
    public static OrganizationDetailsResponse from(
            Organization organization,
            List<OrganizationMemberResponse> members,
            List<ProjectResponse> projects) {
        return new OrganizationDetailsResponse(
                organization.getId().value(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus().name(),
                organization.getCreatedAt(),
                organization.getUpdatedAt(),
                members,
                projects
        );
    }
}
