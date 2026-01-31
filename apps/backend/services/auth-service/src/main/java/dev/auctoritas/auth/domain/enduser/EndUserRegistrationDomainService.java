package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;

/**
 * Domain service for EndUser registration operations.
 *
 * This is a pure domain service that encapsulates business logic for registering
 * new users. It does not interact with infrastructure (repositories, external services).
 *
 * Responsibilities:
 * - Validate password against project policy
 * - Prepare validated registration data
 * - Return RegistrationAttempt for application layer to complete
 */
public class EndUserRegistrationDomainService {

  /**
   * Validates registration data and prepares a registration attempt.
   *
   * This method performs pure business logic:
   * - Validates password against project settings policy
   * - Returns a RegistrationAttempt with validated data
   *
   * The application layer must:
   * - Hash the password
   * - Create the EndUser aggregate with hashed password
   * - Persist the user
   *
   * @param project the project the user belongs to
   * @param settings project settings containing password policy
   * @param email validated email address
   * @param rawPassword raw password string (will be validated, NOT hashed here)
   * @param name user's display name
   * @return RegistrationAttempt containing validated data
   * @throws DomainValidationException if password doesn't meet policy
   */
  public RegistrationAttempt prepareRegistration(
      Project project,
      ProjectSettings settings,
      Email email,
      String rawPassword,
      String name) {

    if (settings == null) {
      throw new DomainValidationException("project_settings_missing");
    }

    // Validate password against project policy
    Password validatedPassword = Password.create(
        rawPassword,
        settings.getMinLength(),
        settings.isRequireUppercase(),
        settings.isRequireLowercase(),
        settings.isRequireNumbers(),
        settings.isRequireSpecialChars());

    // Return attempt for application layer to complete (including password hashing)
    return RegistrationAttempt.of(project, settings, email, validatedPassword, name);
  }
}
