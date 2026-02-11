package com.example.api.application.platformadmin.dto;

import com.example.api.domain.platformadmin.PlatformAdmin;
import com.example.api.domain.platformadmin.PlatformAdminStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for platform admin data.
 */
public record PlatformAdminResponse(
        String id,
        String email,
        String name,
        String status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static PlatformAdminResponse from(PlatformAdmin admin) {
        return new PlatformAdminResponse(
                admin.getId().value(),
                admin.getEmail().value(),
                admin.getName(),
                admin.getStatus().name(),
                admin.getLastLoginAt(),
                admin.getCreatedAt()
        );
    }
}
