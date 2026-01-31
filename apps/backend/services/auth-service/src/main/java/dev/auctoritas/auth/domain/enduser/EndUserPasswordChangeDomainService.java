package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import java.util.List;
import java.util.Objects;

public class EndUserPasswordChangeDomainService {
  private final EndUserCredentialPolicyDomainService credentialPolicyDomainService;

  public EndUserPasswordChangeDomainService(
      EndUserCredentialPolicyDomainService credentialPolicyDomainService) {
    this.credentialPolicyDomainService =
        Objects.requireNonNull(credentialPolicyDomainService, "credential_policy_required");
  }

  public ProjectSettings requireSettings(ProjectSettings settings) {
    return credentialPolicyDomainService.requireSettings(settings);
  }

  public void validatePasswordPolicy(ProjectSettings settings, String rawPassword) {
    credentialPolicyDomainService.validatePasswordPolicy(settings, rawPassword);
  }

  public void enforcePasswordHistory(
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

  public int resolvePasswordHistoryCount(ProjectSettings settings) {
    return credentialPolicyDomainService.resolvePasswordHistoryCount(settings);
  }

  public ChangeValidationResult validateChangeRequest(
      String currentPassword,
      String newPassword) {
    if (currentPassword == null || currentPassword.isBlank()) {
      throw new DomainValidationException("current_password_required");
    }
    if (newPassword == null || newPassword.isBlank()) {
      throw new DomainValidationException("new_password_required");
    }

    return new ChangeValidationResult(currentPassword.trim(), newPassword.trim());
  }

  public record ChangeValidationResult(String currentPassword, String newPassword) {}
}
