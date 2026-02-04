package dev.auctoritas.auth.application.rbac;

import dev.auctoritas.auth.adapter.in.web.RoleCreateRequest;
import dev.auctoritas.auth.adapter.in.web.RolePermissionUpdateRequest;
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
import dev.auctoritas.auth.domain.rbac.Permission;
import dev.auctoritas.auth.domain.rbac.PermissionCode;
import dev.auctoritas.auth.domain.rbac.PermissionRepositoryPort;
import dev.auctoritas.auth.domain.rbac.Role;
import dev.auctoritas.auth.domain.rbac.RoleDeletedEvent;
import dev.auctoritas.auth.domain.rbac.RoleName;
import dev.auctoritas.auth.domain.rbac.RolePermissionRepositoryPort;
import dev.auctoritas.auth.domain.rbac.RoleRepositoryPort;
import dev.auctoritas.auth.domain.rbac.RolePermissionsUpdatedEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  private final PermissionRepositoryPort permissionRepository;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public RoleApplicationService(
      ProjectRepositoryPort projectRepository,
      RoleRepositoryPort roleRepository,
      RolePermissionRepositoryPort rolePermissionRepository,
      PermissionRepositoryPort permissionRepository,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.projectRepository = projectRepository;
    this.roleRepository = roleRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.permissionRepository = permissionRepository;
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
  public RoleSummaryResponse updateRolePermissions(
      UUID orgId,
      UUID projectId,
      UUID roleId,
      ApplicationPrincipal principal,
      RolePermissionUpdateRequest request) {
    enforceOrgAccess(orgId, principal);
    enforceAdminAccess(principal);
    loadProject(orgId, projectId);
    Role role = loadRole(projectId, roleId);

    List<String> requestedCodes = request.permissionCodes() == null
        ? List.of()
        : request.permissionCodes();
    ResolvedPermissions resolved = resolvePermissions(requestedCodes);

    rolePermissionRepository.setPermissions(roleId, resolved.permissions());
    publishPermissionsUpdated(role, resolved.codes());

    int permissionCount = rolePermissionRepository.listByRoleId(roleId).size();
    return toSummary(role, permissionCount);
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

  private ResolvedPermissions resolvePermissions(List<String> permissionCodes) {
    if (permissionCodes == null || permissionCodes.isEmpty()) {
      return new ResolvedPermissions(List.of(), List.of());
    }
    Map<String, Permission> resolved = new LinkedHashMap<>(permissionCodes.size());
    for (String rawCode : permissionCodes) {
      PermissionCode code = PermissionCode.of(rawCode);
      if (resolved.containsKey(code.value())) {
        continue;
      }
      Permission permission = permissionRepository.findByCode(code.value())
          .orElseThrow(() -> new DomainValidationException("permission_code_invalid"));
      resolved.put(code.value(), permission);
    }
    return new ResolvedPermissions(List.copyOf(resolved.values()), List.copyOf(resolved.keySet()));
  }

  private void publishPermissionsUpdated(Role role, List<String> permissionCodes) {
    List<String> codes = permissionCodes == null ? List.of() : List.copyOf(permissionCodes);
    domainEventPublisherPort.publish(
        "role.permissions.updated",
        new RolePermissionsUpdatedEvent(UUID.randomUUID(), role.getId(), codes, Instant.now()));
  }

  private record ResolvedPermissions(List<Permission> permissions, List<String> codes) {
  }
}
