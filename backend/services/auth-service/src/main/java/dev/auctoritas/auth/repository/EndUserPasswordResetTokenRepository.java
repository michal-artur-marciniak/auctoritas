package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.enduser.EndUserPasswordResetToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface EndUserPasswordResetTokenRepository
    extends JpaRepository<EndUserPasswordResetToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EndUserPasswordResetToken> findByTokenHash(String tokenHash);
}
