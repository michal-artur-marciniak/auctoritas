package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EndUserRefreshTokenRepository extends JpaRepository<EndUserRefreshToken, UUID> {
  Optional<EndUserRefreshToken> findByTokenHash(String tokenHash);
}
