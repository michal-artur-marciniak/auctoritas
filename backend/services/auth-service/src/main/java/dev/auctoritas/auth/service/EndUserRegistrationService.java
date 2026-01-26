package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserRegistrationRequest;
import dev.auctoritas.auth.api.EndUserRegistrationResponse;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.entity.enduser.EndUserSession;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserRefreshTokenRepository;
import dev.auctoritas.auth.repository.EndUserRepository;
import dev.auctoritas.auth.repository.EndUserSessionRepository;
import dev.auctoritas.common.dto.PasswordPolicy;
import dev.auctoritas.common.validation.PasswordValidator;
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
public class EndUserRegistrationService {
  private static final int DEFAULT_MAX_PASSWORD_LENGTH = 128;
  private static final int DEFAULT_MIN_UNIQUE = 4;

  private final ApiKeyService apiKeyService;
  private final EndUserRepository endUserRepository;
  private final EndUserSessionRepository endUserSessionRepository;
  private final EndUserRefreshTokenRepository endUserRefreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final JwtService jwtService;
  private final EndUserEmailVerificationService endUserEmailVerificationService;

  public EndUserRegistrationService(
      ApiKeyService apiKeyService,
      EndUserRepository endUserRepository,
      EndUserSessionRepository endUserSessionRepository,
      EndUserRefreshTokenRepository endUserRefreshTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      JwtService jwtService,
      EndUserEmailVerificationService endUserEmailVerificationService) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.endUserRefreshTokenRepository = endUserRefreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
    this.endUserEmailVerificationService = endUserEmailVerificationService;
  }

  @Transactional
  public EndUserRegistrationResponse register(
      String apiKey,
      EndUserRegistrationRequest request,
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

    if (endUserRepository.existsByEmailAndProjectId(email, project.getId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "email_taken");
    }

    validatePassword(settings, password);

    EndUser user = new EndUser();
    user.setProject(project);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setName(trimToNull(request.name()));

    EndUser savedUser = endUserRepository.save(user);
    endUserEmailVerificationService.issueVerificationToken(savedUser);

    Instant refreshExpiresAt = tokenService.getRefreshTokenExpiry();
    String rawRefreshToken = tokenService.generateRefreshToken();
    persistRefreshToken(savedUser, rawRefreshToken, refreshExpiresAt, ipAddress, userAgent);
    persistSession(savedUser, refreshExpiresAt, ipAddress, userAgent);

    String accessToken =
        jwtService.generateEndUserAccessToken(
            savedUser.getId(), project.getId(), savedUser.getEmail(), settings.getAccessTokenTtlSeconds());

    return new EndUserRegistrationResponse(
        new EndUserRegistrationResponse.EndUserSummary(
            savedUser.getId(), savedUser.getEmail(), savedUser.getName(), Boolean.TRUE.equals(savedUser.getEmailVerified())),
        accessToken,
        rawRefreshToken);
  }

  private void validatePassword(ProjectSettings settings, String password) {
    int minLength = settings.getMinLength();
    int minUnique = Math.max(1, Math.min(DEFAULT_MIN_UNIQUE, minLength));
    PasswordPolicy policy =
        new PasswordPolicy(
            minLength,
            DEFAULT_MAX_PASSWORD_LENGTH,
            settings.isRequireUppercase(),
            true,
            settings.isRequireNumbers(),
            settings.isRequireSpecialChars(),
            minUnique);
    PasswordValidator.ValidationResult result = new PasswordValidator(policy).validate(password);
    if (!result.valid()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password_policy_failed");
    }
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
    EndUserSession session = new EndUserSession();
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
