package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserLoginRequest;
import dev.auctoritas.auth.api.EndUserLoginResponse;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.valueobject.Email;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.model.enduser.EndUserSession;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.ports.identity.EndUserRepositoryPort;
import dev.auctoritas.auth.ports.security.JwtProviderPort;
import dev.auctoritas.auth.ports.security.TokenHasherPort;
import dev.auctoritas.auth.ports.identity.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.ports.identity.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.ports.messaging.DomainEventPublisherPort;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles EndUser login and session issuance.
 * Thin application service - delegates business logic to domain entities.
 */
@Service
public class EndUserLoginService {
  private static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;
  private static final int DEFAULT_WINDOW_SECONDS = 900;
  private static final int MIN_WINDOW_SECONDS = 60;

  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserSessionRepositoryPort endUserSessionRepository;
  private final EndUserRefreshTokenRepositoryPort endUserRefreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenHasherPort tokenHasherPort;
  private final JwtProviderPort jwtProviderPort;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public EndUserLoginService(
      ApiKeyService apiKeyService,
      EndUserRepositoryPort endUserRepository,
      EndUserSessionRepositoryPort endUserSessionRepository,
      EndUserRefreshTokenRepositoryPort endUserRefreshTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenHasherPort tokenHasherPort,
      JwtProviderPort jwtProviderPort,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.endUserRefreshTokenRepository = endUserRefreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenHasherPort = tokenHasherPort;
    this.jwtProviderPort = jwtProviderPort;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Transactional
  public EndUserLoginResponse login(
      String apiKey,
      EndUserLoginRequest request,
      String ipAddress,
      String userAgent) {

    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new DomainValidationException("project_settings_missing");
    }

    Email email = Email.of(request.email());
    String password = requireValue(request.password(), "password_required");

    EndUser user =
        endUserRepository
            .findByEmailAndProjectIdForUpdate(email.value(), project.getId())
            .orElseThrow(() -> new DomainValidationException("invalid_credentials"));

    Instant now = Instant.now();
    int maxAttempts = resolveMaxAttempts(settings);
    int windowSeconds = resolveWindowSeconds(settings);

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      boolean locked = user.recordFailedLogin(maxAttempts, windowSeconds, now);
      endUserRepository.save(user);
      if (locked) {
        throw new DomainValidationException("account_locked");
      }
      throw new DomainValidationException("invalid_credentials");
    }

    user.clearFailedAttempts();
    user.validateCanLogin(settings.isRequireVerifiedEmailForLogin(), now);
    endUserRepository.save(user);

    return createSession(user, project, settings, ipAddress, userAgent);
  }

  private EndUserLoginResponse createSession(
      EndUser user, Project project, ProjectSettings settings, String ipAddress, String userAgent) {

    Instant refreshExpiresAt = tokenHasherPort.getRefreshTokenExpiry();
    String rawRefreshToken = tokenHasherPort.generateRefreshToken();

    persistRefreshToken(user, rawRefreshToken, refreshExpiresAt, ipAddress, userAgent);
    persistSession(user, refreshExpiresAt, ipAddress, userAgent);

    String accessToken =
        jwtProviderPort.generateEndUserAccessToken(
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

  private int resolveMaxAttempts(ProjectSettings settings) {
    int configured = settings.getFailedLoginMaxAttempts();
    return configured > 0 ? configured : DEFAULT_MAX_FAILED_ATTEMPTS;
  }

  private int resolveWindowSeconds(ProjectSettings settings) {
    int configured = settings.getFailedLoginWindowSeconds();
    int resolved = configured > 0 ? configured : DEFAULT_WINDOW_SECONDS;
    return Math.max(MIN_WINDOW_SECONDS, resolved);
  }

  private void persistRefreshToken(
      EndUser user,
      String rawToken,
      Instant expiresAt,
      String ipAddress,
      String userAgent) {
    EndUserRefreshToken token = new EndUserRefreshToken();
    token.setUser(user);
    token.setTokenHash(tokenHasherPort.hashToken(rawToken));
    token.setExpiresAt(expiresAt);
    token.setRevoked(false);
    token.setIpAddress(trimToNull(ipAddress));
    token.setUserAgent(trimToNull(userAgent));
    endUserRefreshTokenRepository.save(token);
  }

  private void persistSession(
      EndUser user, Instant expiresAt, String ipAddress, String userAgent) {
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
