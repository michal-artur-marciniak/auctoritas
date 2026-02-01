package dev.auctoritas.auth.application;

import dev.auctoritas.auth.api.EndUserLoginResponse;
import dev.auctoritas.auth.api.OAuthExchangeRequest;
import dev.auctoritas.auth.domain.exception.DomainUnauthorizedException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.enduser.EndUserSession;
import dev.auctoritas.auth.domain.oauth.OAuthExchangeCode;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.domain.oauth.OAuthExchangeCodeRepositoryPort;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OAuthExchangeService {
  private final ApiKeyService apiKeyService;
  private final OAuthExchangeCodeRepositoryPort oauthExchangeCodeRepository;
  private final EndUserRefreshTokenRepositoryPort refreshTokenRepository;
  private final EndUserSessionRepositoryPort endUserSessionRepository;
  private final TokenService tokenService;
  private final JwtService jwtService;
  private final DomainEventPublisherPort domainEventPublisherPort;
  private final TransactionTemplate transactionTemplate;

  public OAuthExchangeService(
      ApiKeyService apiKeyService,
      OAuthExchangeCodeRepositoryPort oauthExchangeCodeRepository,
      EndUserRefreshTokenRepositoryPort refreshTokenRepository,
      EndUserSessionRepositoryPort endUserSessionRepository,
      TokenService tokenService,
      JwtService jwtService,
      DomainEventPublisherPort domainEventPublisherPort,
      PlatformTransactionManager transactionManager) {
    this.apiKeyService = apiKeyService;
    this.oauthExchangeCodeRepository = oauthExchangeCodeRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
    this.domainEventPublisherPort = domainEventPublisherPort;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public EndUserLoginResponse exchange(
      String apiKey, OAuthExchangeRequest request, String ipAddress, String userAgent) {
    ExchangeContext context =
        transactionTemplate.execute(
            status -> exchangeInTransaction(apiKey, request, ipAddress, userAgent));
    if (context == null) {
      throw new IllegalStateException("oauth_exchange_failed");
    }

    String accessToken =
        jwtService.generateEndUserAccessToken(
            context.userId(),
            context.projectId(),
            context.email(),
            context.emailVerified(),
            context.accessTokenTtlSeconds());

    return new EndUserLoginResponse(
        new EndUserLoginResponse.EndUserSummary(
            context.userId(), context.email(), context.name(), context.emailVerified()),
        accessToken,
        context.rawRefreshToken());
  }

  private ExchangeContext exchangeInTransaction(
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

    return new ExchangeContext(
        user.getId(),
        user.getEmail(),
        user.getName(),
        user.isEmailVerified(),
        project.getId(),
        settings.getAccessTokenTtlSeconds(),
        rawRefreshToken);
  }

  private record ExchangeContext(
      java.util.UUID userId,
      String email,
      String name,
      boolean emailVerified,
      java.util.UUID projectId,
      long accessTokenTtlSeconds,
      String rawRefreshToken) {}

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
