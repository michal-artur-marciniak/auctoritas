package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserRefreshRequest;
import dev.auctoritas.auth.api.EndUserRefreshResponse;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.model.enduser.EndUserSession;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.model.enduser.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.ports.messaging.DomainEventPublisherPort;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EndUserRefreshService {
  private final ApiKeyService apiKeyService;
  private final EndUserRefreshTokenRepositoryPort refreshTokenRepository;
  private final EndUserSessionRepositoryPort endUserSessionRepository;
  private final TokenService tokenService;
  private final JwtService jwtService;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public EndUserRefreshService(
      ApiKeyService apiKeyService,
      EndUserRefreshTokenRepositoryPort refreshTokenRepository,
      EndUserSessionRepositoryPort endUserSessionRepository,
      TokenService tokenService,
      JwtService jwtService,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.apiKeyService = apiKeyService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Transactional
  public EndUserRefreshResponse refresh(
      String apiKey,
      EndUserRefreshRequest request,
      String ipAddress,
      String userAgent) {
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    String rawToken = requireValue(request.refreshToken(), "refresh_token_required");
    String tokenHash = tokenService.hashToken(rawToken);

    EndUserRefreshToken existingToken;
    try {
      existingToken =
          refreshTokenRepository
              .findByTokenHash(tokenHash)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.BAD_REQUEST, "invalid_refresh_token"));
    } catch (PessimisticLockException | LockTimeoutException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_refresh_token");
    }

    if (!existingToken.getUser().getProject().getId().equals(project.getId())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid");
    }

    if (existingToken.isRevoked()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh_token_revoked");
    }

    if (existingToken.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh_token_expired");
    }

    EndUser user = existingToken.getUser();
    if (settings.isRequireVerifiedEmailForLogin() && !user.isEmailVerified()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email_not_verified");
    }

    Instant refreshExpiresAt = tokenService.getRefreshTokenExpiry();
    String newRawRefreshToken = tokenService.generateRefreshToken();

    // Use rich domain model's rotate method
    EndUserRefreshToken newToken = existingToken.rotate(
        tokenService.hashToken(newRawRefreshToken),
        Duration.between(Instant.now(), refreshExpiresAt),
        trimToNull(ipAddress),
        trimToNull(userAgent));
    
    // Save both tokens (old one is now revoked, new one is created)
    refreshTokenRepository.save(existingToken);
    refreshTokenRepository.save(newToken);

    // Publish events from both tokens
    existingToken.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    existingToken.clearDomainEvents();
    newToken.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    newToken.clearDomainEvents();

    persistSession(user, refreshExpiresAt, ipAddress, userAgent);

    String accessToken =
        jwtService.generateEndUserAccessToken(
            user.getId(),
            project.getId(),
            user.getEmail(),
            user.isEmailVerified(),
            settings.getAccessTokenTtlSeconds());

    return new EndUserRefreshResponse(accessToken, newRawRefreshToken);
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
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
