package dev.auctoritas.auth.application.port.in.oauth;

import dev.auctoritas.auth.adapter.in.web.EndUserLoginResponse;
import dev.auctoritas.auth.adapter.in.web.OAuthExchangeRequest;

/**
 * Use case for OAuth code exchange.
 */
public interface OAuthExchangeUseCase {
  EndUserLoginResponse exchange(
      String apiKey,
      OAuthExchangeRequest request,
      String ipAddress,
      String userAgent);
}
