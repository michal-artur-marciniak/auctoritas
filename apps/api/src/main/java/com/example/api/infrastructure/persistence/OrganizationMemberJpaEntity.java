package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.OrganizationMemberRole;
import com.example.api.domain.organization.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * JPA entity mapping for organization_members table.
 */
@Entity
@Table(name = "organization_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "email"}))
public class OrganizationMemberJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationMemberRole role;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected OrganizationMemberJpaEntity() {
        // JPA requires no-arg constructor
    }

    public OrganizationMemberJpaEntity(String id,
                                       String organizationId,
                                       String email,
                                       String passwordHash,
                                       String name,
                                       OrganizationMemberRole role,
                                       boolean emailVerified,
                                       OrganizationStatus status,
                                       LocalDateTime lastLoginAt,
                                       LocalDateTime createdAt,
                                       LocalDateTime updatedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.emailVerified = emailVerified;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OrganizationMemberRole getRole() {
        return role;
    }

    public void setRole(OrganizationMemberRole role) {
        this.role = role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public void setStatus(OrganizationStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
