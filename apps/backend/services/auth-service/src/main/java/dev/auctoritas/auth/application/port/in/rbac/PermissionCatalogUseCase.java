package dev.auctoritas.auth.application.port.in.rbac;

import dev.auctoritas.auth.adapter.in.web.PermissionSummaryResponse;
import java.util.List;

/**
 * Use case for listing permission definitions.
 */
public interface PermissionCatalogUseCase {
  List<PermissionSummaryResponse> listPermissions();
}
