package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.oauth.OAuthAuthorizationRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthAuthorizationRequestRepository
    extends JpaRepository<OAuthAuthorizationRequest, UUID> {
  Optional<OAuthAuthorizationRequest> findByStateHash(String stateHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select req from OAuthAuthorizationRequest req where req.stateHash = :stateHash")
  Optional<OAuthAuthorizationRequest> findByStateHashForUpdate(@Param("stateHash") String stateHash);
}
