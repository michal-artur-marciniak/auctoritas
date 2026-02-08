package com.example.api.presentation.project.dto;

import com.example.api.application.project.dto.CreateProjectRequest;
import com.example.api.domain.organization.OrganizationId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a project request.
 */
public record CreateProjectRequestDto(
    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must be at most 100 characters")
    String name,

    @NotBlank(message = "Project slug is required")
    @Size(max = 50, message = "Project slug must be at most 50 characters")
    String slug,

    @Size(max = 500, message = "Description must be at most 500 characters")
    String description
) {

    public CreateProjectRequest toRequest(OrganizationId organizationId) {
        return new CreateProjectRequest(
            organizationId,
            name,
            slug,
            description
        );
    }
}
