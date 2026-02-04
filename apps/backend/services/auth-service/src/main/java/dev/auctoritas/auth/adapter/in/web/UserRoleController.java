package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;
import dev.auctoritas.auth.application.port.in.ApplicationPrincipal;
import dev.auctoritas.auth.application.port.in.rbac.UserRoleAssignmentUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/org/{orgId}/projects/{projectId}/users")
public class UserRoleController {
  private final UserRoleAssignmentUseCase userRoleAssignmentUseCase;

  public UserRoleController(UserRoleAssignmentUseCase userRoleAssignmentUseCase) {
    this.userRoleAssignmentUseCase = userRoleAssignmentUseCase;
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{userId}/roles")
  public ResponseEntity<UserRoleAssignmentResponse> assignRoles(
      @PathVariable UUID orgId,
      @PathVariable UUID projectId,
      @PathVariable UUID userId,
      @Valid @RequestBody UserRoleAssignmentRequest request,
      @AuthenticationPrincipal OrganizationMemberPrincipal principal) {
    return ResponseEntity.ok(userRoleAssignmentUseCase.assignRoles(
        orgId, projectId, userId, toApplicationPrincipal(principal), request));
  }

  private ApplicationPrincipal toApplicationPrincipal(OrganizationMemberPrincipal principal) {
    if (principal == null) {
      return null;
    }
    return new ApplicationPrincipal(
        principal.orgMemberId(), principal.orgId(), principal.email(), principal.role());
  }
}
