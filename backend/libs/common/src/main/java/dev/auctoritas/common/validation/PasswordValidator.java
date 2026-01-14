package dev.auctoritas.common.validation;

import dev.auctoritas.common.config.PasswordConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordValidator {
  private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
  private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
  private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
  private static final Pattern WHITESPACE_PATTERN = Pattern.compile(".*\\s.*");

  private static final java.util.Set<String> COMMON_PASSWORDS = java.util.Set.of(
      "password", "password1", "password123", "12345678", "qwerty123",
      "admin123", "letmein", "welcome1", "monkey123", "dragon123"
  );

  private final PasswordConfig config;

  public ValidationResult validate(String password) {
    if (password == null) {
      List<ValidationError> errors = new ArrayList<>();
      errors.add(ValidationError.TOO_SHORT);
      return ValidationResult.failure(errors);
    }

    var policy = config.getPolicy();
    var errors = new ArrayList<ValidationError>();

    if (password.length() < policy.getMinLength()) {
      errors.add(ValidationError.TOO_SHORT);
    }

    if (password.length() > policy.getMaxLength()) {
      errors.add(ValidationError.TOO_LONG);
    }

    if (policy.isRequireUppercase() && !UPPERCASE_PATTERN.matcher(password).matches()) {
      errors.add(ValidationError.MISSING_UPPERCASE);
    }

    if (policy.isRequireLowercase() && !LOWERCASE_PATTERN.matcher(password).matches()) {
      errors.add(ValidationError.MISSING_LOWERCASE);
    }

    if (policy.isRequireDigit() && !DIGIT_PATTERN.matcher(password).matches()) {
      errors.add(ValidationError.MISSING_DIGIT);
    }

    if (policy.isRequireSpecialChar() && !containsSpecialChar(password, policy.getSpecialChars())) {
      errors.add(ValidationError.MISSING_SPECIAL_CHAR);
    }

    if (WHITESPACE_PATTERN.matcher(password).matches()) {
      errors.add(ValidationError.WHITESPACE);
    }

    if (isCommonPassword(password)) {
      errors.add(ValidationError.COMMON_PASSWORD);
    }

    if (hasSequentialChars(password)) {
      errors.add(ValidationError.SEQUENTIAL_CHARS);
    }

    if (hasRepeatedChars(password)) {
      errors.add(ValidationError.REPEATED_CHARS);
    }

    return errors.isEmpty()
        ? ValidationResult.success()
        : ValidationResult.failure(errors);
  }

  public ValidationResult validateSimple(String password) {
    if (password == null) {
      List<ValidationError> errors = new ArrayList<>();
      errors.add(ValidationError.TOO_SHORT);
      return ValidationResult.failure(errors);
    }

    var policy = config.getPolicy();
    var errors = new ArrayList<ValidationError>();

    if (password.length() < policy.getMinLength()) {
      errors.add(ValidationError.TOO_SHORT);
    }

    if (password.length() > policy.getMaxLength()) {
      errors.add(ValidationError.TOO_LONG);
    }

    if (policy.isRequireUppercase() && !UPPERCASE_PATTERN.matcher(password).matches()) {
      errors.add(ValidationError.MISSING_UPPERCASE);
    }

    if (policy.isRequireLowercase() && !LOWERCASE_PATTERN.matcher(password).matches()) {
      errors.add(ValidationError.MISSING_LOWERCASE);
    }

    if (policy.isRequireDigit() && !DIGIT_PATTERN.matcher(password).matches()) {
      errors.add(ValidationError.MISSING_DIGIT);
    }

    if (policy.isRequireSpecialChar() && !containsSpecialChar(password, policy.getSpecialChars())) {
      errors.add(ValidationError.MISSING_SPECIAL_CHAR);
    }

    if (isCommonPassword(password)) {
      errors.add(ValidationError.COMMON_PASSWORD);
    }

    return errors.isEmpty()
        ? ValidationResult.success()
        : ValidationResult.failure(errors);
  }

  private boolean containsSpecialChar(String password, String specialChars) {
    for (char c : password.toCharArray()) {
      if (specialChars.indexOf(c) >= 0) {
        return true;
      }
    }
    return false;
  }

  private boolean isCommonPassword(String password) {
    return COMMON_PASSWORDS.contains(password.toLowerCase());
  }

  private boolean hasSequentialChars(String password) {
    String lower = password.toLowerCase();
    for (int i = 0; i < lower.length() - 3; i++) {
      char c1 = lower.charAt(i);
      char c2 = lower.charAt(i + 1);
      char c3 = lower.charAt(i + 2);
      char c4 = lower.charAt(i + 3);

      if (c2 == c1 + 1 && c3 == c2 + 1 && c4 == c3 + 1) {
        return true;
      }

      if (c2 == c1 - 1 && c3 == c2 - 1 && c4 == c3 - 1) {
        return true;
      }
    }
    return false;
  }

  private boolean hasRepeatedChars(String password) {
    for (int i = 0; i < password.length() - 4; i++) {
      char c = password.charAt(i);
      if (c == password.charAt(i + 1) &&
          c == password.charAt(i + 2) &&
          c == password.charAt(i + 3) &&
          c == password.charAt(i + 4)) {
        return true;
      }
    }
    return false;
  }

  public int getMinLength() {
    return config.getPolicy().getMinLength();
  }

  public int getMaxLength() {
    return config.getPolicy().getMaxLength();
  }

  public String getSpecialChars() {
    return config.getPolicy().getSpecialChars();
  }
}
