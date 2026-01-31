package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.domain.oauth.OAuthConnection;
import dev.auctoritas.auth.domain.oauth.OAuthConnectionRepositoryPort;
import dev.auctoritas.auth.repository.OAuthConnectionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OAuthConnectionRepository} via {@link OAuthConnectionRepositoryPort}.
 */
@Component
public class OAuthConnectionJpaRepositoryAdapter implements OAuthConnectionRepositoryPort {

  private final OAuthConnectionRepository oAuthConnectionRepository;

  public OAuthConnectionJpaRepositoryAdapter(OAuthConnectionRepository oAuthConnectionRepository) {
    this.oAuthConnectionRepository = oAuthConnectionRepository;
  }

  @Override
  public Optional<OAuthConnection> findByProjectIdAndProviderAndProviderUserId(
      UUID projectId, String provider, String providerUserId) {
    return oAuthConnectionRepository.findByProjectIdAndProviderAndProviderUserId(projectId, provider, providerUserId);
  }

  @Override
  public OAuthConnection save(OAuthConnection connection) {
    return oAuthConnectionRepository.save(connection);
  }
}
