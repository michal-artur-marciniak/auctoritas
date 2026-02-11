package com.example.api.domain.platformadmin;

import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root for platform administrators.
 * Platform admins have cross-tenant access to all organizations, projects, and end users.
 */
public class PlatformAdmin {

    private final PlatformAdminId id;
    private Email email;
    private Password password;
    private String name;
    private PlatformAdminStatus status;
    private LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PlatformAdmin(PlatformAdminId id,
                         Email email,
                         Password password,
                         String name,
                         PlatformAdminStatus status,
                         LocalDateTime lastLoginAt,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "Platform admin ID required");
        this.email = Objects.requireNonNull(email, "Email required");
        this.password = Objects.requireNonNull(password, "Password required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name required");
        }
        this.name = name;
        this.status = Objects.requireNonNull(status, "Status required");
        this.lastLoginAt = lastLoginAt;
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp required");
        this.updatedAt = updatedAt;
    }

    public static PlatformAdmin create(Email email, Password password, String name) {
        return new PlatformAdmin(
                PlatformAdminId.generate(),
                email,
                password,
                name,
                PlatformAdminStatus.ACTIVE,
                null,
                LocalDateTime.now(),
                null
        );
    }

    public void changePassword(Password newPassword) {
        this.password = Objects.requireNonNull(newPassword, "Password required");
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name required");
        }
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeEmail(Email newEmail) {
        this.email = Objects.requireNonNull(newEmail, "Email required");
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = PlatformAdminStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void reactivate() {
        this.status = PlatformAdminStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == PlatformAdminStatus.ACTIVE;
    }

    public PlatformAdminId getId() {
        return id;
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

    public PlatformAdminStatus getStatus() {
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
