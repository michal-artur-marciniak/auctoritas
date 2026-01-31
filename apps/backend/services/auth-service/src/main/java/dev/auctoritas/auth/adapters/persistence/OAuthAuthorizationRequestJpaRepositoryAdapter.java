package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.domain.model.oauth.OAuthAuthorizationRequest;
import dev.auctoritas.auth.ports.oauth.OAuthAuthorizationRequestRepositoryPort;
import dev.auctoritas.auth.repository.OAuthAuthorizationRequestRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OAuthAuthorizationRequestRepository} via {@link OAuthAuthorizationRequestRepositoryPort}.
 */
@Component
public class OAuthAuthorizationRequestJpaRepositoryAdapter implements OAuthAuthorizationRequestRepositoryPort {

  private final OAuthAuthorizationRequestRepository oAuthAuthorizationRequestRepository;

  public OAuthAuthorizationRequestJpaRepositoryAdapter(OAuthAuthorizationRequestRepository oAuthAuthorizationRequestRepository) {
    this.oAuthAuthorizationRequestRepository = oAuthAuthorizationRequestRepository;
  }

  @Override
  public Optional<OAuthAuthorizationRequest> findByStateHash(String stateHash) {
    return oAuthAuthorizationRequestRepository.findByStateHash(stateHash);
  }

  @Override
  public Optional<OAuthAuthorizationRequest> findByStateHashForUpdate(String stateHash) {
    return oAuthAuthorizationRequestRepository.findByStateHashForUpdate(stateHash);
  }

  @Override
  public OAuthAuthorizationRequest save(OAuthAuthorizationRequest request) {
    return oAuthAuthorizationRequestRepository.save(request);
  }

  @Override
  public void delete(OAuthAuthorizationRequest request) {
    oAuthAuthorizationRequestRepository.delete(request);
  }
}
