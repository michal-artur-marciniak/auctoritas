package dev.auctoritas.common.validation;

import java.util.ArrayList;
import java.util.List;

public record ValidationResult(
    boolean valid,
    List<ValidationError> errors
) {
  public static ValidationResult success() {
    return new ValidationResult(true, List.of());
  }

  public static ValidationResult failure(ValidationError error) {
    return new ValidationResult(false, List.of(error));
  }

  public static ValidationResult failure(ValidationError error1, ValidationError error2) {
    List<ValidationError> errors = new ArrayList<>();
    errors.add(error1);
    errors.add(error2);
    return new ValidationResult(false, List.copyOf(errors));
  }

  public static ValidationResult failure(ValidationError... errors) {
    return new ValidationResult(false, List.of(errors));
  }

  public static ValidationResult failure(List<ValidationError> errors) {
    return new ValidationResult(false, List.copyOf(errors));
  }

  public ValidationResult merge(ValidationResult other) {
    if (this.valid) {
      return other;
    }
    if (other.valid()) {
      return this;
    }

    List<ValidationError> mergedErrors = new ArrayList<>();
    mergedErrors.addAll(this.errors);
    mergedErrors.addAll(other.errors);
    return new ValidationResult(false, List.copyOf(mergedErrors));
  }

  public boolean hasError(ValidationError error) {
    return errors.contains(error);
  }

  public List<String> getErrorMessages() {
    return errors.stream()
        .map(ValidationError::getMessage)
        .toList();
  }
}
