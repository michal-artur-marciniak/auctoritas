package com.example.api.domain.organization;

import com.example.api.domain.user.Email;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an organization member invitation.
 */
public class OrganizationInvitation {

    private final OrganizationInvitationId id;
    private final OrganizationId organizationId;
    private final Email email;
    private final OrganizationMemberRole role;
    private final String token;
    private final OrganizationMemberId invitedBy;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;

    public OrganizationInvitation(OrganizationInvitationId id,
                                  OrganizationId organizationId,
                                  Email email,
                                  OrganizationMemberRole role,
                                  String token,
                                  OrganizationMemberId invitedBy,
                                  LocalDateTime expiresAt,
                                  LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "Invitation ID required");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID required");
        this.email = Objects.requireNonNull(email, "Email required");
        this.role = Objects.requireNonNull(role, "Role required");
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Invitation token required");
        }
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiry timestamp required");
        this.token = token;
        this.invitedBy = invitedBy;
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp required");
    }

    public static OrganizationInvitation create(OrganizationId organizationId,
                                                Email email,
                                                OrganizationMemberRole role,
                                                OrganizationMemberId invitedBy,
                                                LocalDateTime expiresAt,
                                                String token) {
        if (expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invitation expiry must be in the future");
        }
        return new OrganizationInvitation(
                OrganizationInvitationId.generate(),
                organizationId,
                email,
                role,
                token,
                invitedBy,
                expiresAt,
                LocalDateTime.now()
        );
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public OrganizationInvitationId getId() {
        return id;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public Email getEmail() {
        return email;
    }

    public OrganizationMemberRole getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }

    public OrganizationMemberId getInvitedBy() {
        return invitedBy;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
