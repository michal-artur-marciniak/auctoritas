package com.example.api.presentation.platformadmin;

import com.example.api.application.platformadmin.CreatePlatformAdminRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a platform admin via REST API.
 */
public record CreatePlatformAdminRequestDto(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String name
) {

    public CreatePlatformAdminRequest toRequest() {
        return new CreatePlatformAdminRequest(email, password, name);
    }
}
