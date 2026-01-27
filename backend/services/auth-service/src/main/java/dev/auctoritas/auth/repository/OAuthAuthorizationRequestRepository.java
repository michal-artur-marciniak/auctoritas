package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.oauth.OAuthAuthorizationRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAuthorizationRequestRepository
    extends JpaRepository<OAuthAuthorizationRequest, UUID> {
  Optional<OAuthAuthorizationRequest> findByStateHash(String stateHash);
}
