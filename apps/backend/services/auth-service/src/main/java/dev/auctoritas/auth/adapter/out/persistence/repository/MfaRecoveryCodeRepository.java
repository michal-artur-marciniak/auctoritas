package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.mfa.MfaRecoveryCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for MFA recovery codes.
 */
@Repository
public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {

  List<MfaRecoveryCode> findByUserId(UUID userId);

  List<MfaRecoveryCode> findByMemberId(UUID memberId);

  Optional<MfaRecoveryCode> findByCodeHash(String codeHash);

  void deleteByUserId(UUID userId);

  void deleteByMemberId(UUID memberId);

  @Modifying
  @Query("UPDATE MfaRecoveryCode r SET r.usedAt = :usedAt WHERE r.id = :id")
  void markAsUsed(@Param("id") UUID id, @Param("usedAt") Instant usedAt);

  List<MfaRecoveryCode> findByUserIdAndUsedAtIsNull(UUID userId);

  List<MfaRecoveryCode> findByMemberIdAndUsedAtIsNull(UUID memberId);
}
