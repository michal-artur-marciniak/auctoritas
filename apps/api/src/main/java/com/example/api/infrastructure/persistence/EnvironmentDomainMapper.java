package com.example.api.infrastructure.persistence;

import com.example.api.domain.environment.Environment;
import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.environment.EnvironmentType;
import com.example.api.domain.project.ProjectId;

/**
 * Maps between domain {@link Environment} and JPA {@link EnvironmentJpaEntity}.
 */
final class EnvironmentDomainMapper {

    private EnvironmentDomainMapper() {
        // Utility class
    }

    static Environment toDomain(EnvironmentJpaEntity entity) {
        return new Environment(
                EnvironmentId.of(entity.getId()),
                ProjectId.of(entity.getProjectId()),
                entity.getEnvironmentType(),
                entity.getCreatedAt()
        );
    }

    static EnvironmentJpaEntity toEntity(Environment environment) {
        return new EnvironmentJpaEntity(
                environment.getId().value(),
                environment.getProjectId().value(),
                environment.getEnvironmentType(),
                environment.getCreatedAt()
        );
    }
}
