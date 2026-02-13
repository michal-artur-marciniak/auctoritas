package com.example.api.domain.organization;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root for organizations.
 */
public class Organization {

    private final OrganizationId id;
    private String name;
    private String slug;
    private OrganizationStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Organization(OrganizationId id,
                        String name,
                        String slug,
                        OrganizationStatus status,
                        LocalDateTime createdAt,
                        LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "Organization ID required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Organization name required");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Organization slug required");
        }
        this.name = name;
        this.slug = slug;
        this.status = Objects.requireNonNull(status, "Status required");
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp required");
        this.updatedAt = updatedAt;
    }

    public static Organization create(String name, String slug) {
        return new Organization(
                OrganizationId.generate(),
                name,
                slug,
                OrganizationStatus.ACTIVE,
                LocalDateTime.now(),
                null
        );
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Organization name required");
        }
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void suspend() {
        this.status = OrganizationStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = OrganizationStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public OrganizationId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
