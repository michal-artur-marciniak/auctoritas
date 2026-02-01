package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.domain.project.ProjectSettingsRepositoryPort;
import dev.auctoritas.auth.adapter.out.persistence.repository.ProjectSettingsRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link ProjectSettingsRepository} via {@link ProjectSettingsRepositoryPort}.
 */
@Component
public class ProjectSettingsJpaRepositoryAdapter implements ProjectSettingsRepositoryPort {

  private final ProjectSettingsRepository projectSettingsRepository;

  public ProjectSettingsJpaRepositoryAdapter(ProjectSettingsRepository projectSettingsRepository) {
    this.projectSettingsRepository = projectSettingsRepository;
  }

  @Override
  public Optional<ProjectSettings> findById(UUID id) {
    return projectSettingsRepository.findById(id);
  }

  @Override
  public ProjectSettings save(ProjectSettings settings) {
    return projectSettingsRepository.save(settings);
  }
}
