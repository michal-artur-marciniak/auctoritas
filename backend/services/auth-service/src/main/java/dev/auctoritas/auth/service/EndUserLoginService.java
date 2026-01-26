package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserLoginRequest;
import dev.auctoritas.auth.api.EndUserLoginResponse;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.entity.enduser.EndUserSession;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserRefreshTokenRepository;
import dev.auctoritas.auth.repository.EndUserRepository;
import dev.auctoritas.auth.repository.EndUserSessionRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EndUserLoginService {
  private static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;
  private static final int DEFAULT_WINDOW_SECONDS = 900;
  private static final int MIN_WINDOW_SECONDS = 60;

  private final ApiKeyService apiKeyService;
  private final EndUserRepository endUserRepository;
  private final EndUserSessionRepository endUserSessionRepository;
  private final EndUserRefreshTokenRepository endUserRefreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final JwtService jwtService;

  public EndUserLoginService(
      ApiKeyService apiKeyService,
      EndUserRepository endUserRepository,
      EndUserSessionRepository endUserSessionRepository,
      EndUserRefreshTokenRepository endUserRefreshTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      JwtService jwtService) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.endUserRefreshTokenRepository = endUserRefreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    String email = normalizeEmail(requireValue(request.email(), "email_required"));
    String password = requireValue(request.password(), "password_required");

    EndUser user =
        endUserRepository
            .findByEmailAndProjectId(email, project.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_credentials"));

    int windowSeconds = resolveWindowSeconds(settings);
    resetExpiredLockout(user, windowSeconds);

    if (isLockedOut(user)) {
      endUserRepository.save(user);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "account_locked");
    }

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      boolean locked = recordFailedAttempt(user, settings, windowSeconds);
      endUserRepository.save(user);
      if (locked) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "account_locked");
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_credentials");
    }

    clearFailedAttempts(user);
    endUserRepository.save(user);

    Instant refreshExpiresAt = tokenService.getRefreshTokenExpiry();
    String rawRefreshToken = tokenService.generateRefreshToken();
    persistRefreshToken(user, rawRefreshToken, refreshExpiresAt, ipAddress, userAgent);
    persistSession(user, refreshExpiresAt, ipAddress, userAgent);

    String accessToken =
        jwtService.generateEndUserAccessToken(
            user.getId(), project.getId(), user.getEmail(), settings.getAccessTokenTtlSeconds());

    return new EndUserLoginResponse(
        new EndUserLoginResponse.EndUserSummary(
            user.getId(), user.getEmail(), user.getName(), Boolean.TRUE.equals(user.getEmailVerified())),
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

  private void resetExpiredLockout(EndUser user, int windowSeconds) {
    Instant now = Instant.now();
    if (user.getLockoutUntil() != null && !user.getLockoutUntil().isAfter(now)) {
      user.setLockoutUntil(null);
    }
    Instant windowStart = user.getFailedLoginWindowStart();
    if (windowStart != null && windowStart.plusSeconds(windowSeconds).isBefore(now)) {
      user.setFailedLoginAttempts(0);
      user.setFailedLoginWindowStart(null);
    }
  }

  private boolean isLockedOut(EndUser user) {
    Instant lockoutUntil = user.getLockoutUntil();
    return lockoutUntil != null && lockoutUntil.isAfter(Instant.now());
  }

  private boolean recordFailedAttempt(EndUser user, ProjectSettings settings, int windowSeconds) {
    Instant now = Instant.now();
    Instant windowStart = user.getFailedLoginWindowStart();
    int attempts = user.getFailedLoginAttempts();

    if (windowStart == null || windowStart.plusSeconds(windowSeconds).isBefore(now)) {
      windowStart = now;
      attempts = 1;
    } else {
      attempts += 1;
    }

    user.setFailedLoginWindowStart(windowStart);
    user.setFailedLoginAttempts(attempts);

    int maxAttempts = resolveMaxAttempts(settings);
    if (attempts >= maxAttempts) {
      user.setLockoutUntil(now.plusSeconds(windowSeconds));
      return true;
    }
    return false;
  }

  private void clearFailedAttempts(EndUser user) {
    user.setFailedLoginAttempts(0);
    user.setFailedLoginWindowStart(null);
    user.setLockoutUntil(null);
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
    endUserRefreshTokenRepository.save(token);
  }

  private void persistSession(
      EndUser user, Instant expiresAt, String ipAddress, String userAgent) {
    EndUserSession session =
        endUserSessionRepository.findByUserId(user.getId()).orElseGet(EndUserSession::new);
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

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
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
