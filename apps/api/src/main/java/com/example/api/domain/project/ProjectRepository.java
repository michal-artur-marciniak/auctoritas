package com.example.api.domain.project;

import com.example.api.domain.organization.OrganizationId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for project persistence.
 */
public interface ProjectRepository {

    Optional<Project> findById(ProjectId id);

    Optional<Project> findBySlugAndOrganizationId(String slug, OrganizationId organizationId);

    List<Project> listByOrganizationId(OrganizationId organizationId);

    Project save(Project project);
}
