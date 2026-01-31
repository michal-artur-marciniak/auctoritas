package dev.auctoritas.auth.domain.model.enduser;

import dev.auctoritas.auth.domain.exception.DomainUnauthorizedException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class EndUserPasswordResetDomainService {
  private final EndUserCredentialPolicyDomainService credentialPolicyDomainService;

  public EndUserPasswordResetDomainService(
      EndUserCredentialPolicyDomainService credentialPolicyDomainService) {
    this.credentialPolicyDomainService =
        Objects.requireNonNull(credentialPolicyDomainService, "credential_policy_required");
  }

  public ProjectSettings requireSettings(ProjectSettings settings) {
    return credentialPolicyDomainService.requireSettings(settings);
  }

  public int resolvePasswordHistoryCount(ProjectSettings settings) {
    return credentialPolicyDomainService.resolvePasswordHistoryCount(settings);
  }

  public void validatePasswordPolicy(ProjectSettings settings, String rawPassword) {
    credentialPolicyDomainService.validatePasswordPolicy(settings, rawPassword);
  }

  public ResetTokenValidationResult validateResetToken(
      EndUserPasswordResetToken token,
      Project project,
      Instant now) {

    if (token == null) {
      throw new DomainValidationException("invalid_reset_token");
    }
    if (project == null || project.getId() == null) {
      throw new DomainUnauthorizedException("api_key_invalid");
    }

    if (!token.belongsToProject(project.getId())) {
      throw new DomainUnauthorizedException("api_key_invalid");
    }
    if (token.isUsed()) {
      throw new DomainValidationException("reset_token_used");
    }
    if (token.isExpired(now)) {
      throw new DomainValidationException("reset_token_expired");
    }

    return new ResetTokenValidationResult(token.getUser());
  }

  public void validatePasswordReuse(
      ProjectSettings settings,
      String rawPassword,
      String currentPasswordHash,
      List<String> recentPasswordHashes,
      java.util.function.BiPredicate<String, String> matcher) {

    credentialPolicyDomainService.enforcePasswordHistory(
        settings,
        rawPassword,
        currentPasswordHash,
        recentPasswordHashes,
        matcher);
  }

  public record ResetTokenValidationResult(EndUser user) {}

}
