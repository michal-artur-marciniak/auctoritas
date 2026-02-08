package com.example.api.presentation.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * REST request DTO for org member login.
 */
public record OrgLoginRequestDto(
        @NotBlank(message = "Organization ID is required")
        String organizationId,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
