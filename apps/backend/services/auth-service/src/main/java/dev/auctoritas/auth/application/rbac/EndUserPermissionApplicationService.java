package dev.auctoritas.auth.application.rbac;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.port.in.rbac.EndUserPermissionUseCase;
import dev.auctoritas.auth.domain.exception.DomainUnauthorizedException;
import org.springframework.stereotype.Service;

@Service
public class EndUserPermissionApplicationService implements EndUserPermissionUseCase {
  private final EndUserPermissionResolver permissionResolver;

  public EndUserPermissionApplicationService(EndUserPermissionResolver permissionResolver) {
    this.permissionResolver = permissionResolver;
  }

  @Override
  public EndUserPermissionResolver.ResolvedPermissions getPermissions(EndUserPrincipal principal) {
    if (principal == null) {
      throw new DomainUnauthorizedException("unauthorized");
    }
    return permissionResolver.resolvePermissions(principal.endUserId());
  }
}
