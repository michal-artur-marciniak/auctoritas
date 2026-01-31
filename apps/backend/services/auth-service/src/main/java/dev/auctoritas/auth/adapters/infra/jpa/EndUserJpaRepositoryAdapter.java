package dev.auctoritas.auth.adapters.infra.jpa;

import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserRepositoryPort;
import dev.auctoritas.auth.repository.EndUserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link EndUserRepository} via {@link EndUserRepositoryPort}.
 */
@Component
public class EndUserJpaRepositoryAdapter implements EndUserRepositoryPort {
  private final EndUserRepository endUserRepository;

  public EndUserJpaRepositoryAdapter(EndUserRepository endUserRepository) {
    this.endUserRepository = endUserRepository;
  }

  @Override
  public boolean existsByEmailAndProjectId(String email, UUID projectId) {
    return endUserRepository.existsByEmailAndProjectId(email, projectId);
  }

  @Override
  public Optional<EndUser> findByEmailAndProjectIdForUpdate(String email, UUID projectId) {
    return endUserRepository.findByEmailAndProjectIdForUpdate(email, projectId);
  }

  @Override
  public Optional<EndUser> findByEmailAndProjectId(String email, UUID projectId) {
    return endUserRepository.findByEmailAndProjectId(email, projectId);
  }

  @Override
  public Optional<EndUser> findByIdAndProjectIdForUpdate(UUID userId, UUID projectId) {
    return endUserRepository.findByIdAndProjectIdForUpdate(userId, projectId);
  }

  @Override
  public EndUser save(EndUser user) {
    return endUserRepository.save(user);
  }
}
