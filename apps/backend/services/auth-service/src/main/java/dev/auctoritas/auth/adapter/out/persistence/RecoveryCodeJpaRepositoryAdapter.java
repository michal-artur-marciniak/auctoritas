package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.adapter.out.persistence.entity.MfaRecoveryCodeEntity;
import dev.auctoritas.auth.adapter.out.persistence.repository.MfaRecoveryCodeRepository;
import dev.auctoritas.auth.domain.mfa.RecoveryCodeRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
  public List<RecoveryCodeEntity> findByUserId(UUID userId) {
    return recoveryCodeRepository.findByUserId(userId).stream()
        .map(this::toDomainEntity)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<RecoveryCodeEntity> findByMemberId(UUID memberId) {
    return recoveryCodeRepository.findByMemberId(memberId).stream()
        .map(this::toDomainEntity)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<RecoveryCodeEntity> findByCodeHash(String codeHash) {
    return recoveryCodeRepository.findByCodeHash(codeHash)
        .map(this::toDomainEntity);
  }

  @Override
  @Transactional
  public List<RecoveryCodeEntity> saveAll(List<RecoveryCodeEntity> codes) {
    List<MfaRecoveryCodeEntity> entities = codes.stream()
        .map(this::toJpaEntity)
        .collect(Collectors.toList());

    List<MfaRecoveryCodeEntity> saved = recoveryCodeRepository.saveAll(entities);

    return saved.stream()
        .map(this::toDomainEntity)
        .collect(Collectors.toList());
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

  private RecoveryCodeEntity toDomainEntity(MfaRecoveryCodeEntity jpaEntity) {
    return new RecoveryCodeEntity(
        jpaEntity.getId(),
        jpaEntity.getUserId(),
        jpaEntity.getMemberId(),
        jpaEntity.getCodeHash(),
        jpaEntity.isUsed(),
        jpaEntity.getUsedAt(),
        jpaEntity.getCreatedAt()
    );
  }

  private MfaRecoveryCodeEntity toJpaEntity(RecoveryCodeEntity domainEntity) {
    MfaRecoveryCodeEntity entity = new MfaRecoveryCodeEntity();
    entity.setId(domainEntity.getId());
    entity.setUserId(domainEntity.getUserId());
    entity.setMemberId(domainEntity.getMemberId());
    entity.setCodeHash(domainEntity.getCodeHash());
    if (domainEntity.isUsed()) {
      entity.setUsedAt(domainEntity.getUsedAt());
    }
    entity.setCreatedAt(domainEntity.getCreatedAt());
    return entity;
  }
}
