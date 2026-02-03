package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.mfa.EndUserMfa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

/**
 * JPA repository for EndUserMfa entities.
 */
@Repository
public interface EndUserMfaRepository extends JpaRepository<EndUserMfa, UUID> {

  Optional<EndUserMfa> findByUserId(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT m FROM EndUserMfa m WHERE m.user.id = :userId")
  Optional<EndUserMfa> findByUserIdForUpdate(@Param("userId") UUID userId);

  boolean existsByUserIdAndEnabledTrue(UUID userId);

  void deleteByUserId(UUID userId);
}
