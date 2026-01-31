package dev.auctoritas.auth.ports.oauth;

import dev.auctoritas.auth.domain.model.oauth.OAuthConnection;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for OAuthConnection persistence operations.
 */
public interface OAuthConnectionRepositoryPort {

  Optional<OAuthConnection> findByProjectIdAndProviderAndProviderUserId(
      UUID projectId, String provider, String providerUserId);

  OAuthConnection save(OAuthConnection connection);
}
