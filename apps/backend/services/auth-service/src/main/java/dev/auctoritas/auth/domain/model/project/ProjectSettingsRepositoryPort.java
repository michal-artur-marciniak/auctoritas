package dev.auctoritas.auth.domain.model.project;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for ProjectSettings persistence operations.
 */
public interface ProjectSettingsRepositoryPort {

  Optional<ProjectSettings> findById(UUID id);

  ProjectSettings save(ProjectSettings settings);
}
