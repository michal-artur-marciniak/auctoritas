package com.example.api.application.organization.dto;

import com.example.api.domain.organization.OrganizationMember;

import java.time.LocalDateTime;

/**
 * Response DTO for organization members.
 */
public record OrganizationMemberResponse(
        String id,
        String organizationId,
        String email,
        String name,
        String role,
        boolean emailVerified,
        String status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static OrganizationMemberResponse from(OrganizationMember member) {
        return new OrganizationMemberResponse(
                member.getId().value(),
                member.getOrganizationId().value(),
                member.getEmail().value(),
                member.getName(),
                member.getRole().name(),
                member.isEmailVerified(),
                member.getStatus().name(),
                member.getCreatedAt(),
                member.getLastLoginAt()
        );
    }
}
