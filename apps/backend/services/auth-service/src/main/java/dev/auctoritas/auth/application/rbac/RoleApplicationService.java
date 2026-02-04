package dev.auctoritas.auth.application.rbac;

import dev.auctoritas.auth.adapter.in.web.RoleCreateRequest;
import dev.auctoritas.auth.adapter.in.web.RoleSummaryResponse;
import dev.auctoritas.auth.adapter.in.web.RoleUpdateRequest;
import dev.auctoritas.auth.application.port.in.ApplicationPrincipal;
import dev.auctoritas.auth.application.port.in.rbac.RoleManagementUseCase;
import dev.auctoritas.auth.application.port.in.rbac.RoleQueryUseCase;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.exception.DomainForbiddenException;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectRepositoryPort;
import dev.auctoritas.auth.domain.rbac.Role;
import dev.auctoritas.auth.domain.rbac.RoleDeletedEvent;
import dev.auctoritas.auth.domain.rbac.RoleName;
import dev.auctoritas.auth.domain.rbac.RolePermissionRepositoryPort;
import dev.auctoritas.auth.domain.rbac.RoleRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service handling role management operations.
 */
@Service
public class RoleApplicationService implements RoleManagementUseCase, RoleQueryUseCase {
  private final ProjectRepositoryPort projectRepository;
  private final RoleRepositoryPort roleRepository;
  private final RolePermissionRepositoryPort rolePermissionRepository;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public RoleApplicationService(
      ProjectRepositoryPort projectRepository,
      RoleRepositoryPort roleRepository,
      RolePermissionRepositoryPort rolePermissionRepository,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.projectRepository = projectRepository;
    this.roleRepository = roleRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Override
  @Transactional
  public RoleSummaryResponse createRole(
      UUID orgId,
      UUID projectId,
      ApplicationPrincipal principal,
      RoleCreateRequest request) {
    enforceOrgAccess(orgId, principal);
    enforceAdminAccess(principal);
    Project project = loadProject(orgId, projectId);

    RoleName roleName = RoleName.of(request.name());
    if (roleRepository.findByNameAndProjectId(roleName.value(), projectId).isPresent()) {
      throw new DomainConflictException("role_name_taken");
    }

    try {
      Role role = Role.create(project, roleName, request.description(), false);
      Role savedRole = roleRepository.save(role);
      publishDomainEvents(savedRole);
      return toSummary(savedRole, 0);
    } catch (DataIntegrityViolationException ex) {
      throw new DomainConflictException("role_name_taken", ex);
    }
  }

  @Override
  @Transactional
  public RoleSummaryResponse updateRole(
      UUID orgId,
      UUID projectId,
      UUID roleId,
      ApplicationPrincipal principal,
      RoleUpdateRequest request) {
    enforceOrgAccess(orgId, principal);
    enforceAdminAccess(principal);
    loadProject(orgId, projectId);
    Role role = loadRole(projectId, roleId);

    if (request.name() != null) {
      RoleName newName = RoleName.of(request.name());
      if (!newName.value().equals(role.getName())
          && roleRepository.findByNameAndProjectId(newName.value(), projectId).isPresent()) {
        throw new DomainConflictException("role_name_taken");
      }
      role.rename(newName);
    }

    if (request.description() != null) {
      role.updateDescription(request.description());
    }

    try {
      Role savedRole = roleRepository.save(role);
      publishDomainEvents(savedRole);
      int permissionCount = rolePermissionRepository.listByRoleId(roleId).size();
      return toSummary(savedRole, permissionCount);
    } catch (DataIntegrityViolationException ex) {
      throw new DomainConflictException("role_name_taken", ex);
    }
  }

  @Override
  @Transactional
  public void deleteRole(
      UUID orgId,
      UUID projectId,
      UUID roleId,
      ApplicationPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    enforceAdminAccess(principal);
    loadProject(orgId, projectId);
    Role role = loadRole(projectId, roleId);

    if (role.isSystem()) {
      throw new DomainValidationException("system_role_immutable");
    }

    roleRepository.delete(role);
    domainEventPublisherPort.publish(
        "role.deleted",
        new RoleDeletedEvent(UUID.randomUUID(), roleId, projectId, Instant.now()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoleSummaryResponse> listRoles(
      UUID orgId,
      UUID projectId,
      ApplicationPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    enforceAdminAccess(principal);
    loadProject(orgId, projectId);
    return roleRepository.listByProjectId(projectId).stream()
        .map(role -> toSummary(role, rolePermissionRepository.listByRoleId(role.getId()).size()))
        .toList();
  }

  private void enforceOrgAccess(UUID orgId, ApplicationPrincipal principal) {
    if (principal == null) {
      throw new IllegalStateException("Authenticated org member principal is required.");
    }
    if (!orgId.equals(principal.orgId())) {
      throw new DomainForbiddenException("org_access_denied");
    }
  }

  private void enforceAdminAccess(ApplicationPrincipal principal) {
    OrganizationMemberRole role = principal.role();
    if (role != OrganizationMemberRole.OWNER && role != OrganizationMemberRole.ADMIN) {
      throw new DomainForbiddenException("insufficient_role");
    }
  }

  private Project loadProject(UUID orgId, UUID projectId) {
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new DomainNotFoundException("project_not_found"));
    if (!orgId.equals(project.getOrganization().getId())) {
      throw new DomainNotFoundException("project_not_found");
    }
    return project;
  }

  private Role loadRole(UUID projectId, UUID roleId) {
    Role role = roleRepository.findById(roleId)
        .orElseThrow(() -> new DomainNotFoundException("role_not_found"));
    if (role.getProject() == null || !projectId.equals(role.getProject().getId())) {
      throw new DomainNotFoundException("role_not_found");
    }
    return role;
  }

  private RoleSummaryResponse toSummary(Role role, int permissionCount) {
    return new RoleSummaryResponse(
        role.getId(),
        role.getName(),
        role.getDescription(),
        role.isSystem(),
        permissionCount,
        role.getCreatedAt(),
        role.getUpdatedAt());
  }

  private void publishDomainEvents(Role role) {
    role.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    role.clearDomainEvents();
  }
}
