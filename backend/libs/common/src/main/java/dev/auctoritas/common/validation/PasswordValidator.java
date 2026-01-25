package dev.auctoritas.common.validation;

import dev.auctoritas.common.dto.PasswordPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates passwords against a configurable password policy.
 */
public class PasswordValidator {
  private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
  private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
  private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
  private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

  private final PasswordPolicy policy;

  public PasswordValidator(PasswordPolicy policy) {
    this.policy = policy;
  }

  /**
   * Creates a validator with default password policy.
   */
  public static PasswordValidator withDefaults() {
    return new PasswordValidator(PasswordPolicy.defaults());
  }

  /**
   * Validates the given password against the policy.
   *
   * @param password the password to validate
   * @return validation result with any error messages
   */
  public ValidationResult validate(String password) {
    List<String> errors = new ArrayList<>();

    if (password == null || password.isEmpty()) {
      errors.add("Password is required");
      return new ValidationResult(false, errors);
    }

    if (password.length() < policy.minLength()) {
      errors.add("Password must be at least " + policy.minLength() + " characters");
    }

    if (password.length() > policy.maxLength()) {
      errors.add("Password must not exceed " + policy.maxLength() + " characters");
    }

    if (policy.requireUppercase() && !UPPERCASE_PATTERN.matcher(password).find()) {
      errors.add("Password must contain at least one uppercase letter");
    }

    if (policy.requireLowercase() && !LOWERCASE_PATTERN.matcher(password).find()) {
      errors.add("Password must contain at least one lowercase letter");
    }

    if (policy.requireNumbers() && !NUMBER_PATTERN.matcher(password).find()) {
      errors.add("Password must contain at least one number");
    }

    if (policy.requireSpecialChars() && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
      errors.add("Password must contain at least one special character");
    }

    if (countUniqueChars(password) < policy.minUniqueChars()) {
      errors.add("Password must contain at least " + policy.minUniqueChars() + " unique characters");
    }

    return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
  }

  private int countUniqueChars(String password) {
    Set<Character> uniqueChars = new HashSet<>();
    for (char c : password.toCharArray()) {
      uniqueChars.add(c);
    }
    return uniqueChars.size();
  }

  /**
   * Result of password validation.
   */
  public record ValidationResult(boolean valid, List<String> errors) {
    public String getFirstError() {
      return errors.isEmpty() ? null : errors.get(0);
    }
  }
}
