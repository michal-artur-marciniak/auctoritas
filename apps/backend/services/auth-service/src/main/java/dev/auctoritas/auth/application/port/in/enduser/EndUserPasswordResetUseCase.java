package dev.auctoritas.auth.application.port.in.enduser;

import dev.auctoritas.auth.adapter.in.web.EndUserPasswordForgotRequest;
import dev.auctoritas.auth.adapter.in.web.EndUserPasswordResetRequest;
import dev.auctoritas.auth.adapter.in.web.EndUserPasswordResetResponse;

/**
 * Use case for EndUser password reset functionality.
 */
public interface EndUserPasswordResetUseCase {
  EndUserPasswordResetResponse requestReset(
      String apiKey,
      EndUserPasswordForgotRequest request,
      String ipAddress,
      String userAgent);

  EndUserPasswordResetResponse resetPassword(String apiKey, EndUserPasswordResetRequest request);
}
