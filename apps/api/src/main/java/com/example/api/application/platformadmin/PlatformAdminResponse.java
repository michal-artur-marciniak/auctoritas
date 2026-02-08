package com.example.api.application.platformadmin;

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
        PlatformAdminStatus status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {

    public static PlatformAdminResponse from(PlatformAdmin admin) {
        return new PlatformAdminResponse(
                admin.getId().value(),
                admin.getEmail().value(),
                admin.getName(),
                admin.getStatus(),
                admin.getLastLoginAt(),
                admin.getCreatedAt()
        );
    }
}
