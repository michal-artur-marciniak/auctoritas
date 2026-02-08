package com.example.api.infrastructure.persistence;

import com.example.api.domain.environment.EnvironmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * JPA entity mapping for project_environments table.
 */
@Entity
@Table(name = "project_environments", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "environment_type"}))
public class EnvironmentJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "environment_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private EnvironmentType environmentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected EnvironmentJpaEntity() {
        // JPA requires no-arg constructor
    }

    public EnvironmentJpaEntity(String id,
                                String projectId,
                                EnvironmentType environmentType,
                                LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.environmentType = environmentType;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(EnvironmentType environmentType) {
        this.environmentType = environmentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
