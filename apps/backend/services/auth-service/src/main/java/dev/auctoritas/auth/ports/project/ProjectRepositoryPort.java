package dev.auctoritas.auth.ports.project;

import dev.auctoritas.auth.entity.project.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for Project persistence operations used by application services.
 */
public interface ProjectRepositoryPort {
  boolean existsBySlugAndOrganizationId(String slug, UUID organizationId);

  Project save(Project project);

  List<Project> findAllByOrganizationId(UUID organizationId);

  Optional<Project> findById(UUID projectId);

  Optional<Project> findByIdWithSettings(UUID projectId);
}
