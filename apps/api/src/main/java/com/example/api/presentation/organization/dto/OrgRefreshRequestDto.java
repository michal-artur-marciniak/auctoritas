package com.example.api.presentation.organization.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for org token refresh.
 */
public record OrgRefreshRequestDto(
        @NotBlank
        String refreshToken
) {
}
