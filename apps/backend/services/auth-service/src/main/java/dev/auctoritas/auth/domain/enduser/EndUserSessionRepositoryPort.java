package dev.auctoritas.auth.domain.enduser;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for EndUserSession persistence operations.
 */
public interface EndUserSessionRepositoryPort {

  List<EndUserSession> findByUserId(UUID userId);

  Optional<EndUserSession> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

  long deleteByUserIdAndIdNot(UUID userId, UUID id);

  boolean existsByIdAndUserId(UUID id, UUID userId);

  void deleteByUserId(UUID userId);

  List<EndUserSession> findByExpiresAtBefore(Instant now);

  void deleteByExpiresAtBefore(Instant now);

  EndUserSession save(EndUserSession session);

  Optional<EndUserSession> findById(UUID id);

  void deleteById(UUID id);
}
