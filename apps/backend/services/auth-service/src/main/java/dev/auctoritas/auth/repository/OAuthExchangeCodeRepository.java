package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.domain.oauth.OAuthExchangeCode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<OAuthExchangeCode> findByCodeHash(String codeHash);
}
