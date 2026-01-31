package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.domain.model.oauth.OAuthExchangeCode;
import dev.auctoritas.auth.domain.model.oauth.OAuthExchangeCodeRepositoryPort;
import dev.auctoritas.auth.repository.OAuthExchangeCodeRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OAuthExchangeCodeRepository} via {@link OAuthExchangeCodeRepositoryPort}.
 */
@Component
public class OAuthExchangeCodeJpaRepositoryAdapter implements OAuthExchangeCodeRepositoryPort {

  private final OAuthExchangeCodeRepository oAuthExchangeCodeRepository;

  public OAuthExchangeCodeJpaRepositoryAdapter(OAuthExchangeCodeRepository oAuthExchangeCodeRepository) {
    this.oAuthExchangeCodeRepository = oAuthExchangeCodeRepository;
  }

  @Override
  public Optional<OAuthExchangeCode> findByCodeHash(String codeHash) {
    return oAuthExchangeCodeRepository.findByCodeHash(codeHash);
  }

  @Override
  public OAuthExchangeCode save(OAuthExchangeCode exchangeCode) {
    return oAuthExchangeCodeRepository.save(exchangeCode);
  }
}
