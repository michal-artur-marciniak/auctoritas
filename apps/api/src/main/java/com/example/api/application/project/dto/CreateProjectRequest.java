package com.example.api.application.project.dto;

import com.example.api.domain.organization.OrganizationId;

/**
 * Request DTO for creating a project.
 */
public record CreateProjectRequest(
    OrganizationId organizationId,
    String name,
    String slug,
    String description
) {}
