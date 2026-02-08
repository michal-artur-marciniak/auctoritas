package com.example.api.domain.project;

import com.example.api.domain.organization.OrganizationId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root for projects.
 */
public class Project {

    private final ProjectId id;
    private final OrganizationId organizationId;
    private String name;
    private String slug;
    private String description;
    private ProjectStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Project(ProjectId id,
                   OrganizationId organizationId,
                   String name,
                   String slug,
                   String description,
                   ProjectStatus status,
                   LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "Project ID required");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name required");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Project slug required");
        }
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.status = Objects.requireNonNull(status, "Status required");
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp required");
        this.updatedAt = updatedAt;
    }

    public static Project create(OrganizationId organizationId, String name, String slug, String description) {
        return new Project(
                ProjectId.generate(),
                organizationId,
                name,
                slug,
                description,
                ProjectStatus.ACTIVE,
                LocalDateTime.now(),
                null
        );
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name required");
        }
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isArchived() {
        return this.status == ProjectStatus.ARCHIVED;
    }

    public ProjectId getId() {
        return id;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
