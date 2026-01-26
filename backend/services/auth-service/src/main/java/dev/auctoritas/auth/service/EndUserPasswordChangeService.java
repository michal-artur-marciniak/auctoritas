package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserPasswordChangeRequest;
import dev.auctoritas.auth.api.EndUserPasswordChangeResponse;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserPasswordHistory;
import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserPasswordHistoryRepository;
import dev.auctoritas.auth.repository.EndUserRefreshTokenRepository;
import dev.auctoritas.auth.repository.EndUserRepository;
import dev.auctoritas.auth.repository.EndUserSessionRepository;
import dev.auctoritas.auth.security.EndUserPrincipal;
import dev.auctoritas.common.dto.PasswordPolicy;
import dev.auctoritas.common.validation.PasswordValidator;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EndUserPasswordChangeService {
  private static final int DEFAULT_MAX_PASSWORD_LENGTH = 128;
  private static final int DEFAULT_MIN_UNIQUE = 4;
  private static final int DEFAULT_PASSWORD_HISTORY_COUNT = 5;

  private final ApiKeyService apiKeyService;
  private final EndUserRepository endUserRepository;
  private final EndUserPasswordHistoryRepository passwordHistoryRepository;
  private final EndUserSessionRepository sessionRepository;
  private final EndUserRefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;

  public EndUserPasswordChangeService(
      ApiKeyService apiKeyService,
      EndUserRepository endUserRepository,
      EndUserPasswordHistoryRepository passwordHistoryRepository,
      EndUserSessionRepository sessionRepository,
      EndUserRefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.passwordHistoryRepository = passwordHistoryRepository;
    this.sessionRepository = sessionRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public EndUserPasswordChangeResponse changePassword(
      String apiKey, EndUserPrincipal principal, EndUserPasswordChangeRequest request) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
    }

    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    if (!project.getId().equals(principal.projectId())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid");
    }
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    String currentPassword = requireValue(request.currentPassword(), "current_password_required");
    String newPassword = requireValue(request.newPassword(), "new_password_required");

    EndUser user =
        endUserRepository
            .findByIdAndProjectIdForUpdate(principal.endUserId(), project.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));

    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_current_password");
    }

    validatePassword(settings, newPassword);
    enforcePasswordHistory(settings, project.getId(), user, newPassword);

    String previousPasswordHash = user.getPasswordHash();
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    endUserRepository.save(user);

    recordPasswordHistory(project, user, previousPasswordHash);

    boolean keptCurrentSession = revokeOtherSessions(user.getId());
    boolean revokedOtherSessions = revokeOtherRefreshTokens(user.getId());

    return new EndUserPasswordChangeResponse(
        "Password changed",
        keptCurrentSession,
        revokedOtherSessions);
  }

  private boolean revokeOtherSessions(UUID userId) {
    return sessionRepository
        .findTopByUserIdOrderByCreatedAtDesc(userId)
        .map(
            current -> {
              sessionRepository.deleteByUserIdAndIdNot(userId, current.getId());
              return true;
            })
        .orElse(false);
  }

  private boolean revokeOtherRefreshTokens(UUID userId) {
    EndUserRefreshToken current =
        refreshTokenRepository.findTopByUserIdAndRevokedFalseOrderByCreatedAtDesc(userId).orElse(null);

    if (current == null) {
      return refreshTokenRepository.revokeActiveByUserId(userId) > 0;
    }

    return refreshTokenRepository.revokeActiveByUserIdExcludingId(userId, current.getId()) > 0;
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

  private void enforcePasswordHistory(
      ProjectSettings settings, UUID projectId, EndUser user, String newPassword) {
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
        .findRecent(projectId, user.getId(), PageRequest.of(0, historyCount - 1))
        .forEach(
            entry -> {
              if (passwordEncoder.matches(newPassword, entry.getPasswordHash())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "password_reuse_not_allowed");
              }
            });
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
