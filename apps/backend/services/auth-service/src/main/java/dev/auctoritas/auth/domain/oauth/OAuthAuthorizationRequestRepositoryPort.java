package dev.auctoritas.auth.domain.oauth;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for OAuthAuthorizationRequest persistence operations.
 */
public interface OAuthAuthorizationRequestRepositoryPort {

  Optional<OAuthAuthorizationRequest> findByStateHash(String stateHash);

  Optional<OAuthAuthorizationRequest> findByStateHashForUpdate(String stateHash);

  OAuthAuthorizationRequest save(OAuthAuthorizationRequest request);

  void delete(OAuthAuthorizationRequest request);
}
