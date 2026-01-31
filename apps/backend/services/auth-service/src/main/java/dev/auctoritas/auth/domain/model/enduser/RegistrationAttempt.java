package dev.auctoritas.auth.domain.model.enduser;

import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import java.util.Objects;

/**
 * Value object representing the result of a registration attempt.
 *
 * Contains all validated data needed by the application layer to complete
 * the registration process, including infrastructure concerns like password hashing.
 */
public record RegistrationAttempt(
    Project project,
    ProjectSettings settings,
    Email email,
    Password validatedPassword,
    String name) {

  /**
   * Compact constructor for validation.
   * Ensures required fields are not null.
   */
  public RegistrationAttempt {
    Objects.requireNonNull(project, "project cannot be null");
    Objects.requireNonNull(settings, "settings cannot be null");
    Objects.requireNonNull(email, "email cannot be null");
    Objects.requireNonNull(validatedPassword, "validatedPassword cannot be null");
  }

  /**
   * Creates a new RegistrationAttempt with validated data.
   */
  public static RegistrationAttempt of(
      Project project,
      ProjectSettings settings,
      Email email,
      Password validatedPassword,
      String name) {
    return new RegistrationAttempt(project, settings, email, validatedPassword, name);
  }

  /**
   * Returns the validated plain-text password.
   * The application layer must hash this before creating the EndUser.
   */
  @Override
  public Password validatedPassword() {
    return validatedPassword;
  }

  @Override
  public String toString() {
    return "RegistrationAttempt{"
        + "project=" + project.getId()
        + ", email=" + email.value()
        + ", password=[VALIDATED_PLAIN]"
        + ", name='" + name + '\''
        + '}';
  }
}
