package com.example.api.application.platformadmin.dto;

import com.example.api.domain.platformadmin.PlatformAdmin;

import java.time.LocalDateTime;

/**
 * Response DTO for platform admin authentication.
 */
public record PlatformAdminAuthResponse(
        String accessToken,
        String refreshToken,
        PlatformAdminResponse admin
) {
}
