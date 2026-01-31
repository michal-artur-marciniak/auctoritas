package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserLoginResponse;
import dev.auctoritas.auth.api.OAuthExchangeRequest;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.entity.enduser.EndUserSession;
import dev.auctoritas.auth.entity.oauth.OAuthExchangeCode;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.ports.identity.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.ports.identity.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.ports.oauth.OAuthExchangeCodeRepositoryPort;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OAuthExchangeService {
  private final ApiKeyService apiKeyService;
  private final OAuthExchangeCodeRepositoryPort oauthExchangeCodeRepository;
  private final EndUserRefreshTokenRepositoryPort refreshTokenRepository;
  private final EndUserSessionRepositoryPort endUserSessionRepository;
  private final TokenService tokenService;
  private final JwtService jwtService;

  public OAuthExchangeService(
      ApiKeyService apiKeyService,
      OAuthExchangeCodeRepositoryPort oauthExchangeCodeRepository,
      EndUserRefreshTokenRepositoryPort refreshTokenRepository,
      EndUserSessionRepositoryPort endUserSessionRepository,
      TokenService tokenService,
      JwtService jwtService) {
    this.apiKeyService = apiKeyService;
    this.oauthExchangeCodeRepository = oauthExchangeCodeRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
  }

  @Transactional
  public EndUserLoginResponse exchange(
      String apiKey, OAuthExchangeRequest request, String ipAddress, String userAgent) {
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    String rawCode = requireValue(request.code(), "oauth_code_required");
    String codeHash = tokenService.hashToken(rawCode);

    OAuthExchangeCode code;
    try {
      code =
          oauthExchangeCodeRepository
              .findByCodeHash(codeHash)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_oauth_code"));
    } catch (PessimisticLockException | LockTimeoutException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_oauth_code");
    }

    if (!code.getProject().getId().equals(project.getId())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid");
    }

    Instant now = Instant.now();
    if (code.getUsedAt() != null || code.getExpiresAt().isBefore(now)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_oauth_code");
    }

    EndUser user = code.getUser();
    if (settings.isRequireVerifiedEmailForLogin() && !user.isEmailVerified()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email_not_verified");
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
    EndUserRefreshToken token = new EndUserRefreshToken();
    token.setUser(user);
    token.setTokenHash(tokenService.hashToken(rawToken));
    token.setExpiresAt(expiresAt);
    token.setRevoked(false);
    token.setIpAddress(trimToNull(ipAddress));
    token.setUserAgent(trimToNull(userAgent));
    refreshTokenRepository.save(token);
  }

  private void persistSession(EndUser user, Instant expiresAt, String ipAddress, String userAgent) {
    List<EndUserSession> sessions = endUserSessionRepository.findByUserId(user.getId());
    EndUserSession session =
        sessions.stream()
            .max(Comparator.comparing(EndUserSession::getCreatedAt))
            .orElseGet(EndUserSession::new);
    session.setUser(user);
    session.setDeviceInfo(buildDeviceInfo(userAgent));
    session.setIpAddress(trimToNull(ipAddress));
    session.setExpiresAt(expiresAt);
    endUserSessionRepository.save(session);
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
