package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;
import dev.auctoritas.auth.application.port.in.ApplicationPrincipal;
import dev.auctoritas.auth.application.port.in.rbac.RoleManagementUseCase;
import dev.auctoritas.auth.application.port.in.rbac.RoleQueryUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/org/{orgId}/projects/{projectId}/roles")
public class RoleController {
  private final RoleManagementUseCase roleManagementUseCase;
  private final RoleQueryUseCase roleQueryUseCase;

  public RoleController(
      RoleManagementUseCase roleManagementUseCase,
      RoleQueryUseCase roleQueryUseCase) {
    this.roleManagementUseCase = roleManagementUseCase;
    this.roleQueryUseCase = roleQueryUseCase;
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping
  public ResponseEntity<List<RoleSummaryResponse>> listRoles(
      @PathVariable UUID orgId,
      @PathVariable UUID projectId,
      @AuthenticationPrincipal OrganizationMemberPrincipal principal) {
    return ResponseEntity.ok(
        roleQueryUseCase.listRoles(orgId, projectId, toApplicationPrincipal(principal)));
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public ResponseEntity<RoleSummaryResponse> createRole(
      @PathVariable UUID orgId,
      @PathVariable UUID projectId,
      @Valid @RequestBody RoleCreateRequest request,
      @AuthenticationPrincipal OrganizationMemberPrincipal principal) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(roleManagementUseCase.createRole(
            orgId, projectId, toApplicationPrincipal(principal), request));
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{roleId}")
  public ResponseEntity<RoleSummaryResponse> updateRole(
      @PathVariable UUID orgId,
      @PathVariable UUID projectId,
      @PathVariable UUID roleId,
      @Valid @RequestBody RoleUpdateRequest request,
      @AuthenticationPrincipal OrganizationMemberPrincipal principal) {
    return ResponseEntity.ok(roleManagementUseCase.updateRole(
        orgId, projectId, roleId, toApplicationPrincipal(principal), request));
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{roleId}")
  public ResponseEntity<Void> deleteRole(
      @PathVariable UUID orgId,
      @PathVariable UUID projectId,
      @PathVariable UUID roleId,
      @AuthenticationPrincipal OrganizationMemberPrincipal principal) {
    roleManagementUseCase.deleteRole(orgId, projectId, roleId, toApplicationPrincipal(principal));
    return ResponseEntity.noContent().build();
  }

  private ApplicationPrincipal toApplicationPrincipal(OrganizationMemberPrincipal principal) {
    if (principal == null) {
      return null;
    }
    return new ApplicationPrincipal(
        principal.orgMemberId(),
        principal.orgId(),
        principal.email(),
        principal.role());
  }
}
