package com.example.api.presentation.organization.dto;

import com.example.api.application.organization.dto.CreateOrganizationRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * REST request DTO for organization creation.
 */
public record CreateOrganizationRequestDto(
        @NotBlank(message = "Organization name is required")
        String name,

        @NotBlank(message = "Organization slug is required")
        @Size(max = 50, message = "Organization slug must be 50 characters or less")
        String slug,

        @NotBlank(message = "Owner email is required")
        @Email(message = "Invalid email format")
        String ownerEmail,

        @NotBlank(message = "Owner password is required")
        @Size(min = 8, message = "Owner password must be at least 8 characters")
        String ownerPassword,

        @NotBlank(message = "Owner name is required")
        String ownerName
) {
    public CreateOrganizationRequest toRequest() {
        return new CreateOrganizationRequest(name, slug, ownerEmail, ownerPassword, ownerName);
    }
}
