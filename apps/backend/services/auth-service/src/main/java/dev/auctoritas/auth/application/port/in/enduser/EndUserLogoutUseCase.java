package dev.auctoritas.auth.application.port.in.enduser;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;

/**
 * Use case for EndUser logout.
 */
public interface EndUserLogoutUseCase {
  void logout(String apiKey, EndUserPrincipal principal);
}
