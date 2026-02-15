package com.example.api.application.platformadmin;

import com.example.api.domain.user.User;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Response DTO for end user summary (cross-tenant view for platform admins).
 */
public record EndUserSummaryResponse(
        String id,
        String email,
        String name,
        boolean banned,
        Optional<String> projectId,
        Optional<String> environmentId,
        LocalDateTime createdAt
) {
    public static EndUserSummaryResponse from(User user) {
        return new EndUserSummaryResponse(
                user.getId().value(),
                user.getEmail().value(),
                user.getName(),
                user.isBanned(),
                user.getProjectId().map(pid -> pid.value()),
                user.getEnvironmentId().map(eid -> eid.value()),
                user.getCreatedAt()
        );
    }
}
