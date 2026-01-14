package dev.auctoritas.common.validation;

public enum ValidationError {
  TOO_SHORT("PASSWORD_TOO_SHORT", "Password must be at least %d characters"),
  TOO_LONG("PASSWORD_TOO_LONG", "Password must not exceed %d characters"),
  MISSING_UPPERCASE("PASSWORD_MISSING_UPPERCASE", "Password must contain at least one uppercase letter"),
  MISSING_LOWERCASE("PASSWORD_MISSING_LOWERCASE", "Password must contain at least one lowercase letter"),
  MISSING_DIGIT("PASSWORD_MISSING_DIGIT", "Password must contain at least one digit"),
  MISSING_SPECIAL_CHAR("PASSWORD_MISSING_SPECIAL_CHAR", "Password must contain at least one special character"),
  COMMON_PASSWORD("PASSWORD_COMMON", "Password is too common"),
  SEQUENTIAL_CHARS("PASSWORD_SEQUENTIAL", "Password contains sequential characters"),
  REPEATED_CHARS("PASSWORD_REPEATED", "Password contains repeated characters"),
  WHITESPACE("PASSWORD_WHITESPACE", "Password cannot contain whitespace");

  private final String code;
  private final String messageTemplate;

  ValidationError(String code, String messageTemplate) {
    this.code = code;
    this.messageTemplate = messageTemplate;
  }

  public String getCode() {
    return code;
  }

  public String getMessage(Object... args) {
    if (args.length > 0) {
      return String.format(messageTemplate, args);
    }
    return messageTemplate;
  }
}
