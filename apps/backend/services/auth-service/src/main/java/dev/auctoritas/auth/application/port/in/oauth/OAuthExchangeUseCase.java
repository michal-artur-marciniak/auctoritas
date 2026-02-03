package dev.auctoritas.auth.application.port.in.oauth;

import dev.auctoritas.auth.adapter.in.web.OAuthExchangeRequest;
import dev.auctoritas.auth.application.port.in.enduser.EndUserLoginResult;

/**
 * Use case for OAuth code exchange.
 */
public interface OAuthExchangeUseCase {
  EndUserLoginResult exchange(
      String apiKey,
      OAuthExchangeRequest request,
      String ipAddress,
      String userAgent);
}
