package dev.auctoritas.common.exception;

import dev.auctoritas.common.dto.ErrorDetail;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends RuntimeException {
  private final List<ErrorDetail> errors;

  public ValidationException(String message) {
    super(message);
    this.errors = new ArrayList<>();
  }

  public ValidationException(String message, List<ErrorDetail> errors) {
    super(message);
    this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
  }

  public ValidationException(List<ErrorDetail> errors) {
    super("Validation failed");
    this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
  }

  public ValidationException(ErrorDetail error) {
    super("Validation failed");
    this.errors = new ArrayList<>();
    if (error != null) {
      this.errors.add(error);
    }
  }

  public List<ErrorDetail> getErrors() {
    return new ArrayList<>(errors);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean hasFieldError(String field) {
    return errors.stream()
        .anyMatch(e -> field.equals(e.field()));
  }

  public ErrorDetail getFirstErrorForField(String field) {
    return errors.stream()
        .filter(e -> field.equals(e.field()))
        .findFirst()
        .orElse(null);
  }

  public String getErrorCodesAsString() {
    return errors.stream()
        .map(ErrorDetail::code)
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
  }
}
