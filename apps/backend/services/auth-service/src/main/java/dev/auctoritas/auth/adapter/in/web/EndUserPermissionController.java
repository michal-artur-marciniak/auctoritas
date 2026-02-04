package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.port.in.rbac.EndUserPermissionUseCase;
import dev.auctoritas.auth.application.rbac.EndUserPermissionResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/permissions")
public class EndUserPermissionController {
  private final EndUserPermissionUseCase endUserPermissionUseCase;

  public EndUserPermissionController(EndUserPermissionUseCase endUserPermissionUseCase) {
    this.endUserPermissionUseCase = endUserPermissionUseCase;
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping
  public ResponseEntity<EndUserPermissionResponse> getPermissions(
      @AuthenticationPrincipal EndUserPrincipal principal) {
    EndUserPermissionResolver.ResolvedPermissions resolved =
        endUserPermissionUseCase.getPermissions(principal);
    return ResponseEntity.ok(new EndUserPermissionResponse(resolved.roles(), resolved.permissions()));
  }
}
