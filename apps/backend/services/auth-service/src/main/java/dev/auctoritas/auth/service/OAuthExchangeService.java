package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserLoginResponse;
import dev.auctoritas.auth.api.OAuthExchangeRequest;
import dev.auctoritas.auth.domain.exception.DomainUnauthorizedException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.model.enduser.EndUserSession;
import dev.auctoritas.auth.domain.model.oauth.OAuthExchangeCode;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.model.enduser.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.ports.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.domain.model.oauth.OAuthExchangeCodeRepositoryPort;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthExchangeService {
  private final ApiKeyService apiKeyService;
  private final OAuthExchangeCodeRepositoryPort oauthExchangeCodeRepository;
  private final EndUserRefreshTokenRepositoryPort refreshTokenRepository;
  private final EndUserSessionRepositoryPort endUserSessionRepository;
  private final TokenService tokenService;
  private final JwtService jwtService;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public OAuthExchangeService(
      ApiKeyService apiKeyService,
      OAuthExchangeCodeRepositoryPort oauthExchangeCodeRepository,
      EndUserRefreshTokenRepositoryPort refreshTokenRepository,
      EndUserSessionRepositoryPort endUserSessionRepository,
      TokenService tokenService,
      JwtService jwtService,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.apiKeyService = apiKeyService;
    this.oauthExchangeCodeRepository = oauthExchangeCodeRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Transactional
  public EndUserLoginResponse exchange(
      String apiKey, OAuthExchangeRequest request, String ipAddress, String userAgent) {
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new DomainValidationException("project_settings_missing");
    }

    String rawCode = requireValue(request.code(), "oauth_code_required");
    String codeHash = tokenService.hashToken(rawCode);

    OAuthExchangeCode code;
    try {
      code =
          oauthExchangeCodeRepository
              .findByCodeHash(codeHash)
              .orElseThrow(
                  () -> new DomainValidationException("invalid_oauth_code"));
    } catch (PessimisticLockException | LockTimeoutException ex) {
      throw new DomainValidationException("invalid_oauth_code");
    }

    if (!code.getProject().getId().equals(project.getId())) {
      throw new DomainUnauthorizedException("api_key_invalid");
    }

    Instant now = Instant.now();
    if (code.getUsedAt() != null || code.getExpiresAt().isBefore(now)) {
      throw new DomainValidationException("invalid_oauth_code");
    }

    EndUser user = code.getUser();
    if (settings.isRequireVerifiedEmailForLogin() && !user.isEmailVerified()) {
      throw new DomainValidationException("email_not_verified");
    }

    code.setUsedAt(now);
    oauthExchangeCodeRepository.save(code);

    Instant refreshExpiresAt = tokenService.getRefreshTokenExpiry();
    String rawRefreshToken = tokenService.generateRefreshToken();
    persistRefreshToken(user, rawRefreshToken, refreshExpiresAt, ipAddress, userAgent);
    persistSession(user, refreshExpiresAt, ipAddress, userAgent);

    String accessToken =
        jwtService.generateEndUserAccessToken(
            user.getId(),
            project.getId(),
            user.getEmail(),
            user.isEmailVerified(),
            settings.getAccessTokenTtlSeconds());

    return new EndUserLoginResponse(
        new EndUserLoginResponse.EndUserSummary(
            user.getId(), user.getEmail(), user.getName(), user.isEmailVerified()),
        accessToken,
        rawRefreshToken);
  }

  private void persistRefreshToken(
      EndUser user,
      String rawToken,
      Instant expiresAt,
      String ipAddress,
      String userAgent) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    EndUserRefreshToken token =
        EndUserRefreshToken.create(
            user,
            tokenService.hashToken(rawToken),
            ttl,
            trimToNull(ipAddress),
            trimToNull(userAgent));
    refreshTokenRepository.save(token);

    // Publish and clear domain events
    token.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    token.clearDomainEvents();
  }

  private void persistSession(EndUser user, Instant expiresAt, String ipAddress, String userAgent) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    EndUserSession session =
        EndUserSession.create(user, trimToNull(ipAddress), buildDeviceInfo(userAgent), ttl);
    endUserSessionRepository.save(session);

    // Publish and clear domain events
    session.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    session.clearDomainEvents();
  }

  private Map<String, Object> buildDeviceInfo(String userAgent) {
    Map<String, Object> info = new HashMap<>();
    String resolvedAgent = trimToNull(userAgent);
    info.put("userAgent", resolvedAgent == null ? "unknown" : resolvedAgent);
    return Map.copyOf(info);
  }

  private String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new DomainValidationException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
