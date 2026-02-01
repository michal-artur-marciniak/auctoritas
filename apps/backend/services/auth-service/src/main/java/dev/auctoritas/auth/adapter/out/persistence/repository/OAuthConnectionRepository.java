package dev.auctoritas.auth.infrastructure.persistence.repository;

import dev.auctoritas.auth.domain.oauth.OAuthConnection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthConnectionRepository extends JpaRepository<OAuthConnection, UUID> {
  Optional<OAuthConnection> findByProjectIdAndProviderAndProviderUserId(
      UUID projectId, String provider, String providerUserId);
}
