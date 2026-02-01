package dev.auctoritas.auth.application;

import dev.auctoritas.auth.adapter.in.web.EndUserPasswordForgotRequest;
import dev.auctoritas.auth.adapter.in.web.EndUserPasswordResetRequest;
import dev.auctoritas.auth.adapter.in.web.EndUserPasswordResetResponse;
import dev.auctoritas.auth.domain.exception.DomainException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.enduser.Password;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserCredentialPolicyDomainService;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordHistory;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordResetDomainService;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordResetToken;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.adapter.out.messaging.DomainEventPublisher;
import dev.auctoritas.auth.application.event.PasswordResetRequestedEvent;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordHistoryRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordResetTokenRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserSessionRepositoryPort;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class EndUserPasswordResetService {
  private static final Logger log = LoggerFactory.getLogger(EndUserPasswordResetService.class);
  private static final String GENERIC_MESSAGE =
      "If an account exists, password reset instructions have been sent";

  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserPasswordResetTokenRepositoryPort resetTokenRepository;
  private final EndUserPasswordHistoryRepositoryPort passwordHistoryRepository;
  private final EndUserRefreshTokenRepositoryPort refreshTokenRepository;
  private final EndUserSessionRepositoryPort endUserSessionRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final DomainEventPublisher domainEventPublisher;
  private final boolean logResetToken;
  private final EndUserPasswordResetDomainService domainService;

  public EndUserPasswordResetService(
      ApiKeyService apiKeyService,
      EndUserRepositoryPort endUserRepository,
      EndUserPasswordResetTokenRepositoryPort resetTokenRepository,
      EndUserPasswordHistoryRepositoryPort passwordHistoryRepository,
      EndUserRefreshTokenRepositoryPort refreshTokenRepository,
      EndUserSessionRepositoryPort endUserSessionRepository,
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
    this.domainService =
        new EndUserPasswordResetDomainService(new EndUserCredentialPolicyDomainService());
  }

  @Transactional
  public EndUserPasswordResetResponse requestReset(
      String apiKey, EndUserPasswordForgotRequest request, String ipAddress, String userAgent) {
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    domainService.requireSettings(project.getSettings());

    String email = normalizeEmail(requireValue(request.email(), "email_required"));
    String emailHash = shortenHash(tokenService.hashToken(email));

    return endUserRepository
        .findByEmailAndProjectId(email, project.getId())
        .map(user -> {
          String userIdAnon = anonymizeUserId(user);
          resetTokenRepository.markUsedByUserIdAndProjectId(user.getId(), project.getId(), Instant.now());
          String rawToken = tokenService.generatePasswordResetToken();
          EndUserPasswordResetToken token = EndUserPasswordResetToken.issue(
              project,
              user,
              tokenService.hashToken(rawToken),
              tokenService.getPasswordResetTokenExpiry(),
              trimToNull(ipAddress),
              trimToNull(userAgent));
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
                  token.getId(),
                  token.getTokenHash(),
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
    ProjectSettings settings = domainService.requireSettings(project.getSettings());

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
                    () -> new DomainValidationException("invalid_reset_token"));
      } catch (PessimisticLockException | LockTimeoutException ex) {
        throw new DomainValidationException("invalid_reset_token");
      }

      EndUserPasswordResetDomainService.ResetTokenValidationResult validation =
          domainService.validateResetToken(resetToken, project, Instant.now());
      EndUser user = validation.user();
      String userIdAnon = anonymizeUserId(user);

      domainService.validatePasswordPolicy(settings, newPassword);
      enforcePasswordHistory(settings, project, user, newPassword);

      String previousPasswordHash = user.getPasswordHash();
      user.setPassword(Password.fromHash(passwordEncoder.encode(newPassword)));
      user.clearFailedAttempts();
      endUserRepository.save(user);

      recordPasswordHistory(project, user, previousPasswordHash);

      refreshTokenRepository.revokeActiveByUserId(user.getId());
      endUserSessionRepository.deleteByUserId(user.getId());

      resetToken.markUsed(Instant.now());
      resetTokenRepository.save(resetToken);

      log.info(
          "password_reset_completed {} {} {} {}",
          kv("projectId", project.getId()),
          kv("userId", userIdAnon),
          kv("token", tokenHashPrefix),
          kv("outcome", "success"));

      return new EndUserPasswordResetResponse("Password reset", null);
    } catch (DomainException ex) {
      String reason = ex.getErrorCode() != null ? ex.getErrorCode() : "unknown";
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
    int historyCount = domainService.resolvePasswordHistoryCount(settings);
    var recentHashes = passwordHistoryRepository
        .findRecent(project.getId(), user.getId(), Math.max(0, historyCount - 1))
        .stream()
        .map(EndUserPasswordHistory::getPasswordHash)
        .toList();
    domainService.validatePasswordReuse(
        settings,
        newPassword,
        user.getPasswordHash(),
        recentHashes,
        passwordEncoder::matches);
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

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
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
