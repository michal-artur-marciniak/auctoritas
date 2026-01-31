package dev.auctoritas.auth.ports.identity;

import dev.auctoritas.auth.entity.enduser.EndUserPasswordHistory;
import java.util.List;
import java.util.UUID;

/**
 * Port for EndUserPasswordHistory persistence operations.
 */
public interface EndUserPasswordHistoryRepositoryPort {

  List<EndUserPasswordHistory> findRecent(UUID projectId, UUID userId, int limit);

  EndUserPasswordHistory save(EndUserPasswordHistory history);
}
