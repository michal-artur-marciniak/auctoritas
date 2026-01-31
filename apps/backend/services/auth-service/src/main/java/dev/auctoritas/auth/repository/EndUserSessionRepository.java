package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.domain.enduser.EndUserSession;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface EndUserSessionRepository extends JpaRepository<EndUserSession, UUID> {
  List<EndUserSession> findByUserId(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EndUserSession> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

  long deleteByUserIdAndIdNot(UUID userId, UUID id);

  boolean existsByIdAndUserId(UUID id, UUID userId);

  void deleteByUserId(UUID userId);

  List<EndUserSession> findByExpiresAtBefore(Instant now);

  void deleteByExpiresAtBefore(Instant now);
}
