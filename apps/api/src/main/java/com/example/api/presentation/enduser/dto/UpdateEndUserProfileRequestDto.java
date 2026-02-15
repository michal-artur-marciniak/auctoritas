package com.example.api.presentation.enduser.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * REST API request DTO for updating an end user's profile.
 *
 * <p>Email uniqueness is enforced within the project and environment scope.
 * The name must be non-blank.</p>
 */
public record UpdateEndUserProfileRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Name is required")
        String name
) {
}
