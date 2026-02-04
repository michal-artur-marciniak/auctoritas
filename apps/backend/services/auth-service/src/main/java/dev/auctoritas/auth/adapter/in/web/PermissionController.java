package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.application.port.in.rbac.PermissionCatalogUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {
  private final PermissionCatalogUseCase permissionCatalogUseCase;

  public PermissionController(PermissionCatalogUseCase permissionCatalogUseCase) {
    this.permissionCatalogUseCase = permissionCatalogUseCase;
  }

  @GetMapping
  public ResponseEntity<List<PermissionSummaryResponse>> listPermissions() {
    return ResponseEntity.ok(permissionCatalogUseCase.listPermissions());
  }
}
