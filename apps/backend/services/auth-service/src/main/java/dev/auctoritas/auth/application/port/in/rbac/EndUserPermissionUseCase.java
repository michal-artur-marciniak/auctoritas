package dev.auctoritas.auth.application.port.in.rbac;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.rbac.EndUserPermissionResolver;

/**
 * Use case for resolving end-user roles and permissions.
 */
public interface EndUserPermissionUseCase {
  EndUserPermissionResolver.ResolvedPermissions getPermissions(EndUserPrincipal principal);
}
