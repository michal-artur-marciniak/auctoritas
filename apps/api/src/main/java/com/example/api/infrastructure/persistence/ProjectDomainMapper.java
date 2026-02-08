package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.project.Project;
import com.example.api.domain.project.ProjectId;
import com.example.api.domain.project.ProjectStatus;

/**
 * Maps between domain {@link Project} and JPA {@link ProjectJpaEntity}.
 */
final class ProjectDomainMapper {

    private ProjectDomainMapper() {
        // Utility class
    }

    static Project toDomain(ProjectJpaEntity entity) {
        return new Project(
                ProjectId.of(entity.getId()),
                OrganizationId.of(entity.getOrganizationId()),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    static ProjectJpaEntity toEntity(Project project) {
        return new ProjectJpaEntity(
                project.getId().value(),
                project.getOrganizationId().value(),
                project.getName(),
                project.getSlug(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
