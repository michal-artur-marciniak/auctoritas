package dev.auctoritas.auth.application.rbac;

import dev.auctoritas.auth.adapter.in.web.PermissionSummaryResponse;
import dev.auctoritas.auth.application.port.in.rbac.PermissionCatalogUseCase;
import dev.auctoritas.auth.domain.rbac.Permission;
import dev.auctoritas.auth.domain.rbac.PermissionRepositoryPort;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service handling permission catalog queries.
 */
@Service
public class PermissionCatalogApplicationService implements PermissionCatalogUseCase {
  private final PermissionRepositoryPort permissionRepository;

  public PermissionCatalogApplicationService(PermissionRepositoryPort permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PermissionSummaryResponse> listPermissions() {
    return permissionRepository.listAll().stream()
        .filter(permission -> permission != null)
        .map(this::toSummary)
        .sorted(
            Comparator.comparing(PermissionSummaryResponse::category, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PermissionSummaryResponse::code, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private PermissionSummaryResponse toSummary(Permission permission) {
    return new PermissionSummaryResponse(
        permission.getCode(),
        permission.getDescription(),
        permission.getCategory());
  }
}
