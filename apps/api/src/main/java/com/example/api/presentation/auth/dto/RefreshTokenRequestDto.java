package com.example.api.presentation.auth.dto;

import com.example.api.application.auth.dto.RefreshTokenRequest;
import jakarta.validation.constraints.NotBlank;

/**
 * REST API request DTO for refresh token.
 */
public record RefreshTokenRequestDto(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {

    public RefreshTokenRequest toRequest() {
        return new RefreshTokenRequest(refreshToken);
    }
}
