package dev.auctoritas.auth.service;

import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.ports.identity.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.ports.identity.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.security.EndUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
    }

    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    if (!resolvedKey.getProject().getId().equals(principal.projectId())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid");
    }

    endUserSessionRepository.deleteByUserId(principal.endUserId());
    endUserRefreshTokenRepository.revokeActiveByUserId(principal.endUserId());
  }
}
