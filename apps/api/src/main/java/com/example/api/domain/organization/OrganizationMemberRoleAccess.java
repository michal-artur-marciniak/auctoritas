package com.example.api.domain.organization;

import com.example.api.domain.organization.exception.OrganizationMemberNotFoundException;

import java.util.Objects;

/**
 * Utility for enforcing organization member role access.
 */
public final class OrganizationMemberRoleAccess {

    private OrganizationMemberRoleAccess() {
        // Utility class
    }

    public static void requireOwner(OrganizationMember member) {
        requireRole(member, OrganizationMemberRole.OWNER);
    }

    public static void requireOwnerOrAdmin(OrganizationMember member) {
        if (member == null) {
            throw new OrganizationMemberNotFoundException();
        }
        if (member.getRole() != OrganizationMemberRole.OWNER
                && member.getRole() != OrganizationMemberRole.ADMIN) {
            throw new IllegalArgumentException("Insufficient role for this action");
        }
    }

    private static void requireRole(OrganizationMember member, OrganizationMemberRole role) {
        Objects.requireNonNull(role, "Role required");
        if (member == null) {
            throw new OrganizationMemberNotFoundException();
        }
        if (member.getRole() != role) {
            throw new IllegalArgumentException("Insufficient role for this action");
        }
    }
}
