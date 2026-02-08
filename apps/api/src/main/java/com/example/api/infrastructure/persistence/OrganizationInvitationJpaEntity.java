package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.OrganizationMemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * JPA entity mapping for organization_invitations table.
 */
@Entity
@Table(name = "organization_invitations",
        uniqueConstraints = @UniqueConstraint(columnNames = "token"))
public class OrganizationInvitationJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationMemberRole role;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "invited_by", length = 36)
    private String invitedBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected OrganizationInvitationJpaEntity() {
        // JPA requires no-arg constructor
    }

    public OrganizationInvitationJpaEntity(String id,
                                           String organizationId,
                                           String email,
                                           OrganizationMemberRole role,
                                           String token,
                                           String invitedBy,
                                           LocalDateTime expiresAt,
                                           LocalDateTime createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.email = email;
        this.role = role;
        this.token = token;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OrganizationMemberRole getRole() {
        return role;
    }

    public void setRole(OrganizationMemberRole role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
