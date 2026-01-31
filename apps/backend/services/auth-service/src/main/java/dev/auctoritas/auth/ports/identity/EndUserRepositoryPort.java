package dev.auctoritas.auth.ports.identity;

import dev.auctoritas.auth.entity.enduser.EndUser;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for EndUser persistence operations used by identity services.
 */
public interface EndUserRepositoryPort {
  boolean existsByEmailAndProjectId(String email, UUID projectId);

  Optional<EndUser> findByEmailAndProjectId(String email, UUID projectId);

  Optional<EndUser> findByEmailAndProjectIdForUpdate(String email, UUID projectId);

  Optional<EndUser> findByIdAndProjectIdForUpdate(UUID userId, UUID projectId);

  EndUser save(EndUser user);
}
