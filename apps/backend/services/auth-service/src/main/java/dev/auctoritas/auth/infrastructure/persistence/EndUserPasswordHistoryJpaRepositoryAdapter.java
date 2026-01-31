package dev.auctoritas.auth.infrastructure.persistence;

import dev.auctoritas.auth.domain.enduser.EndUserPasswordHistory;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordHistoryRepositoryPort;
import dev.auctoritas.auth.infrastructure.persistence.repository.EndUserPasswordHistoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link EndUserPasswordHistoryRepository} via {@link EndUserPasswordHistoryRepositoryPort}.
 */
@Component
public class EndUserPasswordHistoryJpaRepositoryAdapter implements EndUserPasswordHistoryRepositoryPort {

  private final EndUserPasswordHistoryRepository endUserPasswordHistoryRepository;

  public EndUserPasswordHistoryJpaRepositoryAdapter(EndUserPasswordHistoryRepository endUserPasswordHistoryRepository) {
    this.endUserPasswordHistoryRepository = endUserPasswordHistoryRepository;
  }

  @Override
  public List<EndUserPasswordHistory> findRecent(UUID projectId, UUID userId, int limit) {
    return endUserPasswordHistoryRepository.findRecent(projectId, userId, PageRequest.of(0, limit));
  }

  @Override
  public EndUserPasswordHistory save(EndUserPasswordHistory history) {
    return endUserPasswordHistoryRepository.save(history);
  }
}
