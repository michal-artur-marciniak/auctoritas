package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.adapter.out.persistence.repository.MfaRecoveryCodeRepository;
import dev.auctoritas.auth.domain.mfa.MfaRecoveryCode;
import dev.auctoritas.auth.domain.mfa.RecoveryCodeRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter exposing {@link MfaRecoveryCodeRepository} via {@link RecoveryCodeRepositoryPort}.
 * Implements the outbound persistence adapter for recovery codes following Hexagonal Architecture.
 */
@Component
public class RecoveryCodeJpaRepositoryAdapter implements RecoveryCodeRepositoryPort {

  private final MfaRecoveryCodeRepository recoveryCodeRepository;

  public RecoveryCodeJpaRepositoryAdapter(MfaRecoveryCodeRepository recoveryCodeRepository) {
    this.recoveryCodeRepository = recoveryCodeRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MfaRecoveryCode> findByUserId(UUID userId) {
    return recoveryCodeRepository.findByUserId(userId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MfaRecoveryCode> findByMemberId(UUID memberId) {
    return recoveryCodeRepository.findByMemberId(memberId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MfaRecoveryCode> findByCodeHash(String codeHash) {
    return recoveryCodeRepository.findByCodeHash(codeHash);
  }

  @Override
  @Transactional
  public List<MfaRecoveryCode> saveAll(List<MfaRecoveryCode> codes) {
    return recoveryCodeRepository.saveAll(codes);
  }

  @Override
  @Transactional
  public void deleteByUserId(UUID userId) {
    recoveryCodeRepository.deleteByUserId(userId);
  }

  @Override
  @Transactional
  public void deleteByMemberId(UUID memberId) {
    recoveryCodeRepository.deleteByMemberId(memberId);
  }

  @Override
  @Transactional
  public void markAsUsed(UUID id) {
    recoveryCodeRepository.markAsUsed(id, Instant.now());
  }
}
