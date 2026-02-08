package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.project.Project;
import com.example.api.domain.project.ProjectId;
import com.example.api.domain.project.ProjectRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing the domain {@link ProjectRepository} port.
 */
@Repository
public class JpaProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectJpaRepository jpaRepository;

    public JpaProjectRepositoryAdapter(ProjectJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Project> findById(ProjectId id) {
        return jpaRepository.findById(id.value())
                .map(ProjectDomainMapper::toDomain);
    }

    @Override
    public Optional<Project> findBySlugAndOrganizationId(String slug, OrganizationId organizationId) {
        return jpaRepository.findBySlugAndOrganizationId(slug, organizationId.value())
                .map(ProjectDomainMapper::toDomain);
    }

    @Override
    public List<Project> listByOrganizationId(OrganizationId organizationId) {
        return jpaRepository.findByOrganizationId(organizationId.value())
                .stream()
                .map(ProjectDomainMapper::toDomain)
                .toList();
    }

    @Override
    public Project save(Project project) {
        final var entity = ProjectDomainMapper.toEntity(project);
        jpaRepository.save(entity);
        return project;
    }
}
