package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserPasswordForgotRequest;
import dev.auctoritas.auth.api.EndUserPasswordResetRequest;
import dev.auctoritas.auth.api.EndUserPasswordResetResponse;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserPasswordResetToken;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EndUserPasswordResetService {
  private static final int DEFAULT_MAX_PASSWORD_LENGTH = 128;
  private static final int DEFAULT_MIN_UNIQUE = 4;
  private static final String GENERIC_MESSAGE =
      "If an account exists, password reset instructions have been sent";

  private final ApiKeyService apiKeyService;
  private final EndUserRepository endUserRepository;
  private final EndUserPasswordResetTokenRepository resetTokenRepository;
  private final EndUserRefreshTokenRepository refreshTokenRepository;
  private final EndUserSessionRepository endUserSessionRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  public EndUserPasswordResetService(
      ApiKeyService apiKeyService,
      EndUserRepository endUserRepository,
      EndUserPasswordResetTokenRepository resetTokenRepository,
      EndUserRefreshTokenRepository refreshTokenRepository,
      EndUserSessionRepository endUserSessionRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.resetTokenRepository = resetTokenRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
  }

  @Transactional
  public EndUserPasswordResetResponse requestReset(
      String apiKey, EndUserPasswordForgotRequest request) {
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    String email = normalizeEmail(requireValue(request.email(), "email_required"));

    return endUserRepository
        .findByEmailAndProjectId(email, project.getId())
        .map(user -> {
          resetTokenRepository.markUsedByUserId(user.getId(), Instant.now());
          String rawToken = tokenService.generatePasswordResetToken();
          EndUserPasswordResetToken token = new EndUserPasswordResetToken();
          token.setUser(user);
          token.setTokenHash(tokenService.hashToken(rawToken));
          token.setExpiresAt(tokenService.getPasswordResetTokenExpiry());
          resetTokenRepository.save(token);
          return new EndUserPasswordResetResponse(GENERIC_MESSAGE, rawToken);
        })
        .orElseGet(() -> new EndUserPasswordResetResponse(GENERIC_MESSAGE, null));
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

    String rawToken = requireValue(request.token(), "reset_token_required");
    String newPassword = requireValue(request.newPassword(), "password_required");

    EndUserPasswordResetToken resetToken;
    try {
      resetToken =
          resetTokenRepository
              .findByTokenHash(tokenService.hashToken(rawToken))
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_reset_token"));
    } catch (PessimisticLockException | LockTimeoutException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_reset_token");
    }

    if (!resetToken.getUser().getProject().getId().equals(project.getId())) {
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
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setFailedLoginAttempts(0);
    user.setFailedLoginWindowStart(null);
    user.setLockoutUntil(null);
    endUserRepository.save(user);

    refreshTokenRepository.revokeActiveByUserId(user.getId());
    endUserSessionRepository.deleteByUserId(user.getId());

    resetToken.setUsedAt(Instant.now());
    resetTokenRepository.save(resetToken);

    return new EndUserPasswordResetResponse("Password reset", null);
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
}
