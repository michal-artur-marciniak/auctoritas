package dev.auctoritas.auth.application.port.in.enduser;

import dev.auctoritas.auth.adapter.in.web.EndUserLoginRequest;
import dev.auctoritas.auth.adapter.in.web.EndUserLoginResponse;

/**
 * Use case for EndUser login.
 */
public interface EndUserLoginUseCase {
  EndUserLoginResponse login(
      String apiKey, EndUserLoginRequest request, String ipAddress, String userAgent);
}
