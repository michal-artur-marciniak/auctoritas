package dev.auctoritas.auth.application.port.in.oauth;

import java.net.URI;

/**
 * Use case for public OAuth authorization flow.
 */
public interface PublicOAuthAuthorizationUseCase {
  URI authorize(String provider, String apiKey, String redirectUri);

  URI callback(String provider, String code, String state);
}
