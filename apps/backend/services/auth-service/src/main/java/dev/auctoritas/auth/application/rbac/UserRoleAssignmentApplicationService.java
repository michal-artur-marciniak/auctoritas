package dev.auctoritas.auth.application.rbac;

import dev.auctoritas.auth.adapter.in.web.UserRoleAssignmentRequest;
import dev.auctoritas.auth.adapter.in.web.UserRoleAssignmentResponse;
import dev.auctoritas.auth.application.port.in.ApplicationPrincipal;
import dev.auctoritas.auth.application.port.in.rbac.UserRoleAssignmentUseCase;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserRepositoryPort;
import dev.auctoritas.auth.domain.exception.DomainForbiddenException;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectRepositoryPort;
import dev.auctoritas.auth.domain.rbac.Role;
import dev.auctoritas.auth.domain.rbac.RolePermission;
import dev.auctoritas.auth.domain.rbac.RolePermissionRepositoryPort;
import dev.auctoritas.auth.domain.rbac.RoleRepositoryPort;
import dev.auctoritas.auth.domain.rbac.UserRoleAssignmentDomainService;
import dev.auctoritas.auth.domain.rbac.UserRoleRepositoryPort;
import dev.auctoritas.auth.domain.rbac.UserRolesAssignedEvent;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service handling end-user role assignments.
 */
@Service
public class UserRoleAssignmentApplicationService implements UserRoleAssignmentUseCase {
  private final ProjectRepositoryPort projectRepository;
  private final EndUserRepositoryPort endUserRepository;
  private final RoleRepositoryPort roleRepository;
  private final UserRoleRepositoryPort userRoleRepository;
  private final RolePermissionRepositoryPort rolePermissionRepository;
  private final UserRoleAssignmentDomainService userRoleAssignmentDomainService;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public UserRoleAssignmentApplicationService(
      ProjectRepositoryPort projectRepository,
      EndUserRepositoryPort endUserRepository,
      RoleRepositoryPort roleRepository,
      UserRoleRepositoryPort userRoleRepository,
      RolePermissionRepositoryPort rolePermissionRepository,
      UserRoleAssignmentDomainService userRoleAssignmentDomainService,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.projectRepository = projectRepository;
    this.endUserRepository = endUserRepository;
    this.roleRepository = roleRepository;
    this.userRoleRepository = userRoleRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.userRoleAssignmentDomainService = userRoleAssignmentDomainService;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Override
  @Transactional
  public UserRoleAssignmentResponse assignRoles(
      UUID orgId,
      UUID projectId,
      UUID userId,
      ApplicationPrincipal principal,
      UserRoleAssignmentRequest request) {
    enforceOrgAccess(orgId, principal);
    enforceAdminAccess(principal);
    Project project = loadProject(orgId, projectId);

    EndUser user = endUserRepository.findByIdAndProjectIdForUpdate(userId, project.getId())
        .orElseThrow(() -> new DomainNotFoundException("user_not_found"));

    List<Role> roles = resolveRoles(project.getId(), request == null ? null : request.roleIds());
    userRoleAssignmentDomainService.validateProjectAlignment(user, roles);

    List<Role> assignedRoles = userRoleRepository.replaceAssignments(user.getId(), roles).stream()
        .map(assignment -> assignment.getRole())
        .filter(role -> role != null)
        .filter(role -> role.getId() != null)
        .toList();

    publishUserRolesAssigned(user.getId(), project.getId(), assignedRoles);

    return new UserRoleAssignmentResponse(
        assignedRoles.stream()
            .map(role -> new UserRoleAssignmentResponse.RoleSummary(role.getId(), role.getName()))
            .toList(),
        resolvePermissionCodes(assignedRoles));
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

  private List<Role> resolveRoles(UUID projectId, List<UUID> roleIds) {
    if (roleIds == null || roleIds.isEmpty()) {
      return List.of();
    }
    Map<UUID, Role> roles = new LinkedHashMap<>(roleIds.size());
    for (UUID roleId : roleIds) {
      if (roleId == null) {
        throw new DomainValidationException("role_id_required");
      }
      Role role = roleRepository.findById(roleId)
          .orElseThrow(() -> new DomainNotFoundException("role_not_found"));
      if (role.getProject() == null || !projectId.equals(role.getProject().getId())) {
        throw new DomainValidationException("role_project_mismatch");
      }
      roles.putIfAbsent(roleId, role);
    }
    return List.copyOf(roles.values());
  }

  private List<String> resolvePermissionCodes(List<Role> roles) {
    if (roles == null || roles.isEmpty()) {
      return List.of();
    }
    Map<String, String> codes = new LinkedHashMap<>();
    for (Role role : roles) {
      if (role == null) {
        continue;
      }
      List<RolePermission> permissions = rolePermissionRepository.listByRoleId(role.getId());
      for (RolePermission mapping : permissions) {
        if (mapping == null || mapping.getPermission() == null) {
          continue;
        }
        String code = mapping.getPermission().getCode();
        if (code != null && !code.isBlank()) {
          codes.putIfAbsent(code, code);
        }
      }
    }
    return List.copyOf(codes.keySet());
  }

  private void publishUserRolesAssigned(UUID userId, UUID projectId, List<Role> roles) {
    List<UUID> roleIds = roles == null
        ? List.of()
        : roles.stream()
            .filter(role -> role != null && role.getId() != null)
            .map(Role::getId)
            .toList();
    domainEventPublisherPort.publish(
        "user.roles.assigned",
        new UserRolesAssignedEvent(UUID.randomUUID(), userId, projectId, roleIds, Instant.now()));
  }
}
