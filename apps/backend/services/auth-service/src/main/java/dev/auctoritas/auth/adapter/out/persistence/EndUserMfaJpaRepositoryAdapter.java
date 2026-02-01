package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.adapter.out.persistence.repository.EndUserMfaRepository;
import dev.auctoritas.auth.domain.mfa.EndUserMfa;
import dev.auctoritas.auth.domain.mfa.EndUserMfaRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter exposing {@link EndUserMfaRepository} via {@link EndUserMfaRepositoryPort}.
 * Implements the outbound persistence adapter for EndUserMfa following Hexagonal Architecture.
 */
@Component
public class EndUserMfaJpaRepositoryAdapter implements EndUserMfaRepositoryPort {

  private final EndUserMfaRepository endUserMfaRepository;

  public EndUserMfaJpaRepositoryAdapter(EndUserMfaRepository endUserMfaRepository) {
    this.endUserMfaRepository = endUserMfaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<EndUserMfa> findByUserId(UUID userId) {
    return endUserMfaRepository.findByUserId(userId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<EndUserMfa> findByUserIdForUpdate(UUID userId) {
    return endUserMfaRepository.findByUserIdForUpdate(userId);
  }

  @Override
  @Transactional
  public EndUserMfa save(EndUserMfa mfa) {
    return endUserMfaRepository.save(mfa);
  }

  @Override
  @Transactional
  public void delete(EndUserMfa mfa) {
    endUserMfaRepository.delete(mfa);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isEnabledByUserId(UUID userId) {
    return endUserMfaRepository.existsByUserIdAndEnabledTrue(userId);
  }
}
