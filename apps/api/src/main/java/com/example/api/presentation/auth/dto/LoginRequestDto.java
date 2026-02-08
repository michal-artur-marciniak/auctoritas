package com.example.api.presentation.auth.dto;

import com.example.api.application.auth.dto.LoginRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * REST API request DTO for user login.
 */
public record LoginRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {

    /**
     * Converts to an application-layer request.
     */
    public LoginRequest toRequest() {
        return new LoginRequest(email, password);
    }
}
