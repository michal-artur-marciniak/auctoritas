package dev.auctoritas.auth.adapters.infra.jpa;

import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.ports.project.ProjectRepositoryPort;
import dev.auctoritas.auth.repository.ProjectRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link ProjectRepository} via {@link ProjectRepositoryPort}.
 */
@Component
public class ProjectJpaRepositoryAdapter implements ProjectRepositoryPort {
  private final ProjectRepository projectRepository;

  public ProjectJpaRepositoryAdapter(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  @Override
  public boolean existsBySlugAndOrganizationId(String slug, UUID organizationId) {
    return projectRepository.existsBySlugAndOrganizationId(slug, organizationId);
  }

  @Override
  public Project save(Project project) {
    return projectRepository.save(project);
  }

  @Override
  public List<Project> findAllByOrganizationId(UUID organizationId) {
    return projectRepository.findAllByOrganizationId(organizationId);
  }

  @Override
  public Optional<Project> findById(UUID projectId) {
    return projectRepository.findById(projectId);
  }

  @Override
  public Optional<Project> findByIdWithSettings(UUID projectId) {
    return projectRepository.findByIdWithSettings(projectId);
  }
}
