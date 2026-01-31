package dev.auctoritas.auth.ports.oauth;

import dev.auctoritas.auth.domain.model.oauth.OAuthExchangeCode;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for OAuthExchangeCode persistence operations.
 */
public interface OAuthExchangeCodeRepositoryPort {

  Optional<OAuthExchangeCode> findByCodeHash(String codeHash);

  OAuthExchangeCode save(OAuthExchangeCode exchangeCode);
}
