package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.shared.security.PasswordPolicy;
import dev.auctoritas.auth.shared.security.PasswordValidator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import org.springframework.stereotype.Component;

@Component
public class EndUserCredentialPolicyDomainService {
  private static final int DEFAULT_MAX_PASSWORD_LENGTH = 128;
  private static final int DEFAULT_MIN_UNIQUE = 4;
  private static final int DEFAULT_PASSWORD_HISTORY_COUNT = 5;

  public ProjectSettings requireSettings(ProjectSettings settings) {
    if (settings == null) {
      throw new DomainValidationException("project_settings_missing");
    }
    return settings;
  }

  public void validatePasswordPolicy(ProjectSettings settings, String rawPassword) {
    ProjectSettings resolved = requireSettings(settings);
    PasswordValidator.ValidationResult result =
        new PasswordValidator(buildPolicy(resolved)).validate(rawPassword);
    if (!result.valid()) {
      throw new DomainValidationException("password_policy_failed");
    }
  }

  public int resolvePasswordHistoryCount(ProjectSettings settings) {
    ProjectSettings resolved = requireSettings(settings);
    int configured = resolved.getPasswordHistoryCount();
    return configured > 0 ? configured : DEFAULT_PASSWORD_HISTORY_COUNT;
  }

  public void enforcePasswordHistory(
      ProjectSettings settings,
      String rawPassword,
      String currentPasswordHash,
      List<String> recentPasswordHashes,
      BiPredicate<String, String> matcher) {

    ProjectSettings resolved = requireSettings(settings);
    Objects.requireNonNull(matcher, "password_matcher_required");

    int historyCount = resolvePasswordHistoryCount(resolved);
    if (historyCount <= 1) {
      if (matches(rawPassword, currentPasswordHash, matcher)) {
        throw new DomainValidationException("password_reuse_not_allowed");
      }
      return;
    }

    if (matches(rawPassword, currentPasswordHash, matcher)) {
      throw new DomainValidationException("password_reuse_not_allowed");
    }

    if (recentPasswordHashes == null || recentPasswordHashes.isEmpty()) {
      return;
    }

    for (String hash : recentPasswordHashes) {
      if (matches(rawPassword, hash, matcher)) {
        throw new DomainValidationException("password_reuse_not_allowed");
      }
    }
  }

  private PasswordPolicy buildPolicy(ProjectSettings settings) {
    int minLength = settings.getMinLength();
    int minUnique = Math.max(1, Math.min(DEFAULT_MIN_UNIQUE, minLength));
    return new PasswordPolicy(
        minLength,
        DEFAULT_MAX_PASSWORD_LENGTH,
        settings.isRequireUppercase(),
        settings.isRequireLowercase(),
        settings.isRequireNumbers(),
        settings.isRequireSpecialChars(),
        minUnique);
  }

  private boolean matches(
      String rawPassword,
      String storedHash,
      BiPredicate<String, String> matcher) {

    if (rawPassword == null || rawPassword.isBlank()) {
      return false;
    }
    if (storedHash == null || storedHash.isBlank()) {
      return false;
    }
    return matcher.test(rawPassword, storedHash);
  }
}
