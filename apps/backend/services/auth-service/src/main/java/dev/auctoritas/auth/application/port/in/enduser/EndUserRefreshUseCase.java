package dev.auctoritas.auth.application.port.in.enduser;

import dev.auctoritas.auth.adapter.in.web.EndUserRefreshRequest;
import dev.auctoritas.auth.adapter.in.web.EndUserRefreshResponse;

/**
 * Use case for refreshing EndUser access tokens.
 */
public interface EndUserRefreshUseCase {
  EndUserRefreshResponse refresh(
      String apiKey,
      EndUserRefreshRequest request,
      String ipAddress,
      String userAgent);
}
