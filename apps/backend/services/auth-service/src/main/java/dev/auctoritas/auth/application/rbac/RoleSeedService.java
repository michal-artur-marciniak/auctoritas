package dev.auctoritas.auth.application.rbac;

import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.rbac.Role;
import dev.auctoritas.auth.domain.rbac.RoleName;
import dev.auctoritas.auth.domain.rbac.RoleRepositoryPort;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds default roles for a project.
 */
@Service
public class RoleSeedService {
  private static final String ADMIN_ROLE_NAME = "admin";
  private static final String USER_ROLE_NAME = "user";
  private static final String ADMIN_ROLE_DESCRIPTION = "Default admin role";
  private static final String USER_ROLE_DESCRIPTION = "Default user role";

  private final RoleRepositoryPort roleRepositoryPort;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public RoleSeedService(
      RoleRepositoryPort roleRepositoryPort,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.roleRepositoryPort = roleRepositoryPort;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Transactional
  public void createDefaultRoles(Project project) {
    Objects.requireNonNull(project, "project_required");
    createIfMissing(project, ADMIN_ROLE_NAME, ADMIN_ROLE_DESCRIPTION, true);
    createIfMissing(project, USER_ROLE_NAME, USER_ROLE_DESCRIPTION, true);
  }

  private void createIfMissing(Project project, String name, String description, boolean system) {
    if (roleRepositoryPort.findByNameAndProjectId(name, project.getId()).isPresent()) {
      return;
    }

    Role role = Role.create(project, RoleName.of(name), description, system);
    Role savedRole = roleRepositoryPort.save(role);
    publishDomainEvents(savedRole);
  }

  private void publishDomainEvents(Role role) {
    role.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    role.clearDomainEvents();
  }
}
