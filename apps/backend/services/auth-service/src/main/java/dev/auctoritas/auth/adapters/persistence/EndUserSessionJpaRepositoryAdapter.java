package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.entity.enduser.EndUserSession;
import dev.auctoritas.auth.ports.identity.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.repository.EndUserSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link EndUserSessionRepository} via {@link EndUserSessionRepositoryPort}.
 */
@Component
public class EndUserSessionJpaRepositoryAdapter implements EndUserSessionRepositoryPort {

  private final EndUserSessionRepository endUserSessionRepository;

  public EndUserSessionJpaRepositoryAdapter(EndUserSessionRepository endUserSessionRepository) {
    this.endUserSessionRepository = endUserSessionRepository;
  }

  @Override
  public List<EndUserSession> findByUserId(UUID userId) {
    return endUserSessionRepository.findByUserId(userId);
  }

  @Override
  public Optional<EndUserSession> findTopByUserIdOrderByCreatedAtDesc(UUID userId) {
    return endUserSessionRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
  }

  @Override
  public long deleteByUserIdAndIdNot(UUID userId, UUID id) {
    return endUserSessionRepository.deleteByUserIdAndIdNot(userId, id);
  }

  @Override
  public boolean existsByIdAndUserId(UUID id, UUID userId) {
    return endUserSessionRepository.existsByIdAndUserId(id, userId);
  }

  @Override
  public void deleteByUserId(UUID userId) {
    endUserSessionRepository.deleteByUserId(userId);
  }

  @Override
  public List<EndUserSession> findByExpiresAtBefore(Instant now) {
    return endUserSessionRepository.findByExpiresAtBefore(now);
  }

  @Override
  public void deleteByExpiresAtBefore(Instant now) {
    endUserSessionRepository.deleteByExpiresAtBefore(now);
  }

  @Override
  public EndUserSession save(EndUserSession session) {
    return endUserSessionRepository.save(session);
  }

  @Override
  public Optional<EndUserSession> findById(UUID id) {
    return endUserSessionRepository.findById(id);
  }

  @Override
  public void deleteById(UUID id) {
    endUserSessionRepository.deleteById(id);
  }
}
