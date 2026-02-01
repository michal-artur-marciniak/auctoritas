package dev.auctoritas.auth.application.port.in.enduser;

import dev.auctoritas.auth.adapter.in.web.EndUserPasswordChangeRequest;
import dev.auctoritas.auth.adapter.in.web.EndUserPasswordChangeResponse;
import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import java.util.UUID;

/**
 * Use case for EndUser password change.
 */
public interface EndUserPasswordChangeUseCase {
  EndUserPasswordChangeResponse changePassword(
      String apiKey,
      EndUserPrincipal principal,
      UUID currentSessionId,
      EndUserPasswordChangeRequest request);
}
