package com.example.api.domain.organization;

import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an organization member.
 */
public class OrganizationMember {

    private final OrganizationMemberId id;
    private final OrganizationId organizationId;
    private Email email;
    private Password password;
    private String name;
    private OrganizationMemberRole role;
    private boolean emailVerified;
    private OrganizationStatus status;
    private LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrganizationMember(OrganizationMemberId id,
                              OrganizationId organizationId,
                              Email email,
                              Password password,
                              String name,
                              OrganizationMemberRole role,
                              boolean emailVerified,
                              OrganizationStatus status,
                              LocalDateTime lastLoginAt,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "Member ID required");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID required");
        this.email = Objects.requireNonNull(email, "Email required");
        this.password = Objects.requireNonNull(password, "Password required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name required");
        }
        this.name = name;
        this.role = Objects.requireNonNull(role, "Role required");
        this.status = Objects.requireNonNull(status, "Status required");
        this.emailVerified = emailVerified;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp required");
        this.updatedAt = updatedAt;
    }

    public static OrganizationMember createOwner(OrganizationId organizationId,
                                                 Email email,
                                                 Password password,
                                                 String name) {
        return new OrganizationMember(
                OrganizationMemberId.generate(),
                organizationId,
                email,
                password,
                name,
                OrganizationMemberRole.OWNER,
                false,
                OrganizationStatus.ACTIVE,
                null,
                LocalDateTime.now(),
                null
        );
    }

    public void markEmailVerified() {
        this.emailVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeRole(OrganizationMemberRole role) {
        this.role = Objects.requireNonNull(role, "Role required");
        this.updatedAt = LocalDateTime.now();
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public OrganizationMemberId getId() {
        return id;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public Email getEmail() {
        return email;
    }

    public Password getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public OrganizationMemberRole getRole() {
        return role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
