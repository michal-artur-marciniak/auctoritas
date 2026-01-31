package dev.auctoritas.auth.application.port.out.oauth;

import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeDetails;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeUrlRequest;
import dev.auctoritas.auth.service.oauth.OAuthTokenExchangeRequest;
import dev.auctoritas.auth.service.oauth.OAuthUserInfo;

/**
 * Port for OAuth provider interactions used by auth application services.
 */
public interface OAuthProviderPort {
  /** Provider key used in requests, persisted connections, and oauth_config. */
  String name();

  /** Returns provider settings for building an authorize URL (or throws if not configured/enabled). */
  OAuthAuthorizeDetails getAuthorizeDetails(ProjectSettings settings);

  /** Builds a provider authorize URL for redirecting the user. */
  String buildAuthorizeUrl(OAuthAuthorizeDetails details, OAuthAuthorizeUrlRequest request);

  /** Exchanges the authorization code and fetches provider user info. */
  OAuthUserInfo exchangeAuthorizationCode(ProjectSettings settings, OAuthTokenExchangeRequest request);
}
