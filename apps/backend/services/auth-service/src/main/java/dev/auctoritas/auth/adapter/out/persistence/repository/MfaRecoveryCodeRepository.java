package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.adapter.out.persistence.entity.MfaRecoveryCodeEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;

/**
 * JPA repository for MFA recovery codes.
 */
@Repository
public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCodeEntity, UUID> {

  List<MfaRecoveryCodeEntity> findByUserId(UUID userId);

  List<MfaRecoveryCodeEntity> findByMemberId(UUID memberId);

  Optional<MfaRecoveryCodeEntity> findByCodeHash(String codeHash);

  void deleteByUserId(UUID userId);

  void deleteByMemberId(UUID memberId);

  @Modifying
  @Query("UPDATE MfaRecoveryCodeEntity r SET r.usedAt = :usedAt WHERE r.id = :id")
  void markAsUsed(@Param("id") UUID id, @Param("usedAt") Instant usedAt);

  List<MfaRecoveryCodeEntity> findByUserIdAndUsedAtIsNull(UUID userId);

  List<MfaRecoveryCodeEntity> findByMemberIdAndUsedAtIsNull(UUID memberId);
}
