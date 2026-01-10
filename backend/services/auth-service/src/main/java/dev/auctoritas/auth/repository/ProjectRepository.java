package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.common.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
  Optional<Project> findBySlugAndOrganizationId(String slug, UUID organizationId);

  boolean existsBySlugAndOrganizationId(String slug, UUID organizationId);

  List<Project> findByOrganizationIdAndStatus(UUID organizationId, ProjectStatus status);

  @Query("SELECT p FROM Project p WHERE p.organization.id = :orgId")
  List<Project> findAllByOrganizationId(@Param("orgId") UUID organizationId);
}
