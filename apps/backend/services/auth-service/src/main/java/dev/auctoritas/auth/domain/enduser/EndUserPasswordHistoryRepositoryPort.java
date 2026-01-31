package dev.auctoritas.auth.domain.enduser;

import java.util.List;
import java.util.UUID;

/**
 * Port for EndUserPasswordHistory persistence operations.
 */
public interface EndUserPasswordHistoryRepositoryPort {

  List<EndUserPasswordHistory> findRecent(UUID projectId, UUID userId, int limit);

  EndUserPasswordHistory save(EndUserPasswordHistory history);
}
