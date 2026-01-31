package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserPasswordChangeRequest;
import dev.auctoritas.auth.api.EndUserPasswordChangeResponse;
import dev.auctoritas.auth.domain.exception.DomainUnauthorizedException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.enduser.Password;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserCredentialPolicyDomainService;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordChangeDomainService;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordHistory;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordHistoryRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.infrastructure.security.EndUserPrincipal;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EndUserPasswordChangeService {
  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserPasswordHistoryRepositoryPort passwordHistoryRepository;
  private final EndUserSessionRepositoryPort sessionRepository;
  private final EndUserRefreshTokenRepositoryPort refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final EndUserPasswordChangeDomainService domainService;

  public EndUserPasswordChangeService(
      ApiKeyService apiKeyService,
      EndUserRepositoryPort endUserRepository,
      EndUserPasswordHistoryRepositoryPort passwordHistoryRepository,
      EndUserSessionRepositoryPort sessionRepository,
      EndUserRefreshTokenRepositoryPort refreshTokenRepository,
      PasswordEncoder passwordEncoder) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.passwordHistoryRepository = passwordHistoryRepository;
    this.sessionRepository = sessionRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.domainService =
        new EndUserPasswordChangeDomainService(new EndUserCredentialPolicyDomainService());
  }

  @Transactional
  public EndUserPasswordChangeResponse changePassword(
      String apiKey,
      EndUserPrincipal principal,
      UUID currentSessionId,
      EndUserPasswordChangeRequest request) {
    if (principal == null) {
      throw new DomainUnauthorizedException("unauthorized");
    }

    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    if (!project.getId().equals(principal.projectId())) {
      throw new DomainUnauthorizedException("api_key_invalid");
    }
    ProjectSettings settings = domainService.requireSettings(project.getSettings());

    EndUserPasswordChangeDomainService.ChangeValidationResult validation =
        domainService.validateChangeRequest(
            request.currentPassword(),
            request.newPassword());
    String currentPassword = validation.currentPassword();
    String newPassword = validation.newPassword();

    EndUser user =
        endUserRepository
            .findByIdAndProjectIdForUpdate(principal.endUserId(), project.getId())
            .orElseThrow(() -> new DomainUnauthorizedException("unauthorized"));

    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new DomainValidationException("invalid_current_password");
    }

    domainService.validatePasswordPolicy(settings, newPassword);
    enforcePasswordHistory(settings, project.getId(), user, newPassword);

    String previousPasswordHash = user.getPasswordHash();
    user.setPassword(Password.fromHash(passwordEncoder.encode(newPassword)));
    endUserRepository.save(user);

    recordPasswordHistory(project, user, previousPasswordHash);

    boolean keptCurrentSession =
        currentSessionId != null && sessionRepository.existsByIdAndUserId(currentSessionId, user.getId());
    boolean revokedOtherSessions =
        keptCurrentSession && revokeOtherSessions(user.getId(), currentSessionId);
    revokeOtherRefreshTokens(user.getId());

    return new EndUserPasswordChangeResponse(
        "Password changed",
        keptCurrentSession,
        revokedOtherSessions);
  }

  private boolean revokeOtherSessions(UUID userId, UUID currentSessionId) {
    if (currentSessionId == null) {
      return false;
    }
    return sessionRepository.deleteByUserIdAndIdNot(userId, currentSessionId) > 0;
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
    int historyCount = domainService.resolvePasswordHistoryCount(settings);
    var recentHashes = passwordHistoryRepository
        .findRecent(projectId, user.getId(), Math.max(0, historyCount - 1))
        .stream()
        .map(EndUserPasswordHistory::getPasswordHash)
        .toList();
    domainService.enforcePasswordHistory(
        settings,
        newPassword,
        user.getPasswordHash(),
        recentHashes,
        passwordEncoder::matches);
  }

}
