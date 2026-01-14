package dev.auctoritas.common.security;

import dev.auctoritas.common.dto.JwtClaims;

import java.util.Set;

public record JwtPrincipal(
    String subject,
    String orgId,
    String projectId,
    String role,
    String type,
    Set<String> permissions
) {
    public static JwtPrincipal fromClaims(JwtClaims claims) {
        return new JwtPrincipal(
            claims.subject(),
            claims.orgId(),
            claims.projectId(),
            claims.role(),
            claims.type(),
            claims.permissions()
        );
    }

    public boolean isOrgMember() {
        return "org_member".equals(type);
    }

    public boolean isEndUser() {
        return "end_user".equals(type);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean hasAnyPermission(String... requiredPermissions) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        for (String permission : requiredPermissions) {
            if (permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }
}
