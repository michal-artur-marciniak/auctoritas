package com.example.api.presentation.user.dto;

import com.example.api.application.user.dto.UpdateUserRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * REST API request DTO for updating a user profile.
 */
public record UpdateUserRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Name is required")
        String name
) {

    public UpdateUserRequest toRequest(String userId) {
        return new UpdateUserRequest(userId, email, name);
    }
}
