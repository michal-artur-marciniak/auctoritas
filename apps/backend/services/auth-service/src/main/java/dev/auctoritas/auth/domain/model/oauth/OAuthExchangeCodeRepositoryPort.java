package dev.auctoritas.auth.domain.model.oauth;

import java.util.Optional;

/**
 * Port for OAuthExchangeCode persistence operations.
 */
public interface OAuthExchangeCodeRepositoryPort {

  Optional<OAuthExchangeCode> findByCodeHash(String codeHash);

  OAuthExchangeCode save(OAuthExchangeCode exchangeCode);
}
