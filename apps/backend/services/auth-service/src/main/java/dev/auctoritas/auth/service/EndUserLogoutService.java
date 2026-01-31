package dev.auctoritas.auth.service;

import dev.auctoritas.auth.domain.exception.DomainUnauthorizedException;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.infrastructure.security.EndUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EndUserLogoutService {
  private final ApiKeyService apiKeyService;
  private final EndUserSessionRepositoryPort endUserSessionRepository;
  private final EndUserRefreshTokenRepositoryPort endUserRefreshTokenRepository;

  public EndUserLogoutService(
      ApiKeyService apiKeyService,
      EndUserSessionRepositoryPort endUserSessionRepository,
      EndUserRefreshTokenRepositoryPort endUserRefreshTokenRepository) {
    this.apiKeyService = apiKeyService;
    this.endUserSessionRepository = endUserSessionRepository;
    this.endUserRefreshTokenRepository = endUserRefreshTokenRepository;
  }

  @Transactional
  public void logout(String apiKey, EndUserPrincipal principal) {
    if (principal == null) {
      throw new DomainUnauthorizedException("unauthorized");
    }

    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    if (!resolvedKey.getProject().getId().equals(principal.projectId())) {
      throw new DomainUnauthorizedException("api_key_invalid");
    }

    endUserSessionRepository.deleteByUserId(principal.endUserId());
    endUserRefreshTokenRepository.revokeActiveByUserId(principal.endUserId());
  }
}
