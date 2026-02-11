package com.example.api.presentation.platformadmin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for platform admin token refresh request.
 */
public record PlatformAdminRefreshRequestDto(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
