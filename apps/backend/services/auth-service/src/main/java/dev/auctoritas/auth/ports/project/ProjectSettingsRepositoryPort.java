package dev.auctoritas.auth.ports.project;

import dev.auctoritas.auth.entity.project.ProjectSettings;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for ProjectSettings persistence operations.
 */
public interface ProjectSettingsRepositoryPort {

  Optional<ProjectSettings> findById(UUID id);

  ProjectSettings save(ProjectSettings settings);
}
