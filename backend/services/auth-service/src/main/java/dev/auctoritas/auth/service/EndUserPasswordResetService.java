package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserPasswordForgotRequest;
import dev.auctoritas.auth.api.EndUserPasswordResetRequest;
import dev.auctoritas.auth.api.EndUserPasswordResetResponse;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserPasswordHistory;
import dev.auctoritas.auth.entity.enduser.EndUserPasswordResetToken;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.messaging.DomainEventPublisher;
import dev.auctoritas.auth.messaging.PasswordResetRequestedEvent;
import dev.auctoritas.auth.repository.EndUserPasswordHistoryRepository;
import dev.auctoritas.auth.repository.EndUserRefreshTokenRepository;
import dev.auctoritas.auth.repository.EndUserPasswordResetTokenRepository;
import dev.auctoritas.auth.repository.EndUserRepository;
import dev.auctoritas.auth.repository.EndUserSessionRepository;
import dev.auctoritas.common.dto.PasswordPolicy;
import dev.auctoritas.common.validation.PasswordValidator;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class EndUserPasswordResetService {
  private static final Logger log = LoggerFactory.getLogger(EndUserPasswordResetService.class);
  private static final int DEFAULT_MAX_PASSWORD_LENGTH = 128;
  private static final int DEFAULT_MIN_UNIQUE = 4;
  private static final int DEFAULT_PASSWORD_HISTORY_COUNT = 5;
  private static final String GENERIC_MESSAGE =
      "If an account exists, password reset instructions have been sent";

  private final ApiKeyService apiKeyService;
  private final EndUserRepository endUserRepository;
  private final EndUserPasswordResetTokenRepository resetTokenRepository;
  private final EndUserPasswordHistoryRepository passwordHistoryRepository;
  private final EndUserRefreshTokenRepository refreshTokenRepository;
  private final EndUserSessionRepository endUserSessionRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final DomainEventPublisher domainEventPublisher;
  private final boolean logResetToken;

  public EndUserPasswordResetService(
      ApiKeyService apiKeyService,
      EndUserRepository endUserRepository,
      EndUserPasswordResetTokenRepository resetTokenRepository,
      EndUserPasswordHistoryRepository passwordHistoryRepository,
      EndUserRefreshTokenRepository refreshTokenRepository,
      EndUserSessionRepository endUserSessionRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      DomainEventPublisher domainEventPublisher,
      @Value("${auctoritas.auth.password-reset.log-token:false}") boolean logResetToken) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.resetTokenRepository = resetTokenRepository;
    this.passwordHistoryRepository = passwordHistoryRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.domainEventPublisher = domainEventPublisher;
    this.logResetToken = logResetToken;
  }

  @Transactional
  public EndUserPasswordResetResponse requestReset(
      String apiKey, EndUserPasswordForgotRequest request, String ipAddress, String userAgent) {
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    String email = normalizeEmail(requireValue(request.email(), "email_required"));
    String emailHash = shortenHash(tokenService.hashToken(email));

    return endUserRepository
        .findByEmailAndProjectId(email, project.getId())
        .map(user -> {
          String userIdAnon = anonymizeUserId(user);
          resetTokenRepository.markUsedByUserIdAndProjectId(user.getId(), project.getId(), Instant.now());
          String rawToken = tokenService.generatePasswordResetToken();
          EndUserPasswordResetToken token = new EndUserPasswordResetToken();
          token.setProject(project);
          token.setUser(user);
          token.setTokenHash(tokenService.hashToken(rawToken));
          token.setExpiresAt(tokenService.getPasswordResetTokenExpiry());
          token.setIpAddress(trimToNull(ipAddress));
          token.setUserAgent(trimToNull(userAgent));
          resetTokenRepository.save(token);

          log.info(
              "password_reset_requested {} {} {}",
              kv("projectId", project.getId()),
              kv("userId", userIdAnon),
              kv("outcome", "issued"));

          PasswordResetRequestedEvent event =
              new PasswordResetRequestedEvent(
                  project.getId(),
                  user.getId(),
                  user.getEmail(),
                  rawToken,
                  token.getExpiresAt(),
                  trimToNull(ipAddress),
                  trimToNull(userAgent));
          try {
            domainEventPublisher.publish(PasswordResetRequestedEvent.EVENT_TYPE, event);
          } catch (RuntimeException ex) {
            log.warn(
                "password_reset_event_publish_failed {} {}",
                kv("projectId", project.getId()),
                kv("userId", userIdAnon),
                ex);
          }

          if (logResetToken) {
            log.info(
                "Stub password reset email projectId={} userId={} email={} resetToken={} expiresAt={}",
                project.getId(),
                user.getId(),
                user.getEmail(),
                rawToken,
                token.getExpiresAt());
          }

          return new EndUserPasswordResetResponse(GENERIC_MESSAGE, null);
        })
        .orElseGet(
            () -> {
              log.info(
                  "password_reset_requested {} {} {}",
                  kv("projectId", project.getId()),
                  kv("emailHash", emailHash),
                  kv("outcome", "email_not_found"));
              return new EndUserPasswordResetResponse(GENERIC_MESSAGE, null);
            });
  }

  @Transactional
  public EndUserPasswordResetResponse resetPassword(
      String apiKey, EndUserPasswordResetRequest request) {
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    String rawToken = request.token();
    String tokenHashPrefix = null;
    if (rawToken != null && !rawToken.isBlank()) {
      tokenHashPrefix = shortenHash(tokenService.hashToken(rawToken.trim()));
    }

    try {
      rawToken = requireValue(request.token(), "reset_token_required");
      String newPassword = requireValue(request.newPassword(), "password_required");

      String tokenHash = tokenService.hashToken(rawToken);
      tokenHashPrefix = shortenHash(tokenHash);

      EndUserPasswordResetToken resetToken;
      try {
        resetToken =
            resetTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_reset_token"));
      } catch (PessimisticLockException | LockTimeoutException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_reset_token");
      }

      if (!resetToken.getUser().getProject().getId().equals(project.getId())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid");
      }

      if (!resetToken.getProject().getId().equals(project.getId())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid");
      }

      if (resetToken.getUsedAt() != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reset_token_used");
      }

      if (resetToken.getExpiresAt().isBefore(Instant.now())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reset_token_expired");
      }

      validatePassword(settings, newPassword);

      EndUser user = resetToken.getUser();
      String userIdAnon = anonymizeUserId(user);

      enforcePasswordHistory(settings, project, user, newPassword);

      String previousPasswordHash = user.getPasswordHash();
      user.setPasswordHash(passwordEncoder.encode(newPassword));
      user.setFailedLoginAttempts(0);
      user.setFailedLoginWindowStart(null);
      user.setLockoutUntil(null);
      endUserRepository.save(user);

      recordPasswordHistory(project, user, previousPasswordHash);

      refreshTokenRepository.revokeActiveByUserId(user.getId());
      endUserSessionRepository.deleteByUserId(user.getId());

      resetToken.setUsedAt(Instant.now());
      resetTokenRepository.save(resetToken);

      log.info(
          "password_reset_completed {} {} {} {}",
          kv("projectId", project.getId()),
          kv("userId", userIdAnon),
          kv("token", tokenHashPrefix),
          kv("outcome", "success"));

      return new EndUserPasswordResetResponse("Password reset", null);
    } catch (ResponseStatusException ex) {
      String reason = ex.getReason() != null ? ex.getReason() : "unknown";
      log.warn(
          "password_reset_failed {} {} {} {}",
          kv("projectId", project.getId()),
          kv("token", tokenHashPrefix),
          kv("outcome", "failed"),
          kv("reason", reason));
      throw ex;
    }
  }

  private void enforcePasswordHistory(
      ProjectSettings settings, Project project, EndUser user, String newPassword) {
    int historyCount = resolvePasswordHistoryCount(settings);
    if (historyCount <= 1) {
      if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password_reuse_not_allowed");
      }
      return;
    }

    if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password_reuse_not_allowed");
    }

    passwordHistoryRepository
        .findRecent(project.getId(), user.getId(), PageRequest.of(0, historyCount - 1))
        .forEach(
            entry -> {
              if (passwordEncoder.matches(newPassword, entry.getPasswordHash())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "password_reuse_not_allowed");
              }
            });
  }

  private void recordPasswordHistory(Project project, EndUser user, String previousPasswordHash) {
    if (previousPasswordHash == null || previousPasswordHash.isBlank()) {
      return;
    }
    EndUserPasswordHistory history = new EndUserPasswordHistory();
    history.setProject(project);
    history.setUser(user);
    history.setPasswordHash(previousPasswordHash);
    passwordHistoryRepository.save(history);
  }

  private int resolvePasswordHistoryCount(ProjectSettings settings) {
    int configured = settings.getPasswordHistoryCount();
    return configured > 0 ? configured : DEFAULT_PASSWORD_HISTORY_COUNT;
  }

  private void validatePassword(ProjectSettings settings, String password) {
    int minLength = settings.getMinLength();
    int minUnique = Math.max(1, Math.min(DEFAULT_MIN_UNIQUE, minLength));
    PasswordPolicy policy =
        new PasswordPolicy(
            minLength,
            DEFAULT_MAX_PASSWORD_LENGTH,
            settings.isRequireUppercase(),
            settings.isRequireLowercase(),
            settings.isRequireNumbers(),
            settings.isRequireSpecialChars(),
            minUnique);
    PasswordValidator.ValidationResult result = new PasswordValidator(policy).validate(password);
    if (!result.valid()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password_policy_failed");
    }
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

  private String anonymizeUserId(EndUser user) {
    if (user == null || user.getId() == null) {
      return null;
    }
    return shortenHash(tokenService.hashToken(user.getId().toString()));
  }

  private String shortenHash(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.length() <= 12 ? trimmed : trimmed.substring(0, 12);
  }
}
