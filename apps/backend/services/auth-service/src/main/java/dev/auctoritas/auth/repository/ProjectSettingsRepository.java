package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ProjectSettingsRepository extends JpaRepository<ProjectSettings, UUID> {}
