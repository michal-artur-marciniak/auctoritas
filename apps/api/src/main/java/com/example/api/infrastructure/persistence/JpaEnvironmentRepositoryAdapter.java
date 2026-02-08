package com.example.api.infrastructure.persistence;

import com.example.api.domain.environment.Environment;
import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.environment.EnvironmentRepository;
import com.example.api.domain.environment.EnvironmentType;
import com.example.api.domain.project.ProjectId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing the domain {@link EnvironmentRepository} port.
 */
@Repository
public class JpaEnvironmentRepositoryAdapter implements EnvironmentRepository {

    private final EnvironmentJpaRepository jpaRepository;

    public JpaEnvironmentRepositoryAdapter(EnvironmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Environment> listByProjectId(ProjectId projectId) {
        return jpaRepository.findByProjectId(projectId.value())
                .stream()
                .map(EnvironmentDomainMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Environment> findByProjectIdAndType(ProjectId projectId, EnvironmentType environmentType) {
        return jpaRepository.findByProjectIdAndEnvironmentType(projectId.value(), environmentType.name())
                .map(EnvironmentDomainMapper::toDomain);
    }

    @Override
    public Environment save(Environment environment) {
        final var entity = EnvironmentDomainMapper.toEntity(environment);
        jpaRepository.save(entity);
        return environment;
    }
}
