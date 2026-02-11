package com.example.api.presentation.platformadmin;

import com.example.api.application.platformadmin.UpdatePlatformAdminProfileRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating platform admin profile.
 */
public record UpdatePlatformAdminProfileRequestDto(
        @Email(message = "Invalid email format")
        String email,

        @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
        String name,

        String currentPassword,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {

    public UpdatePlatformAdminProfileRequest toRequest() {
        return new UpdatePlatformAdminProfileRequest(email, name, currentPassword, newPassword);
    }
}
