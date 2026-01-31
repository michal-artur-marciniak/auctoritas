package dev.auctoritas.auth.domain.model.enduser.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.enduser.EndUserRegistrationDomainService;
import dev.auctoritas.auth.domain.model.enduser.RegistrationAttempt;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.domain.model.enduser.Email;
import dev.auctoritas.auth.domain.model.project.Slug;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for EndUserRegistrationDomainService.
 *
 * These are pure unit tests with no Spring context - testing domain logic only.
 */
class EndUserRegistrationDomainServiceTest {

  private EndUserRegistrationDomainService domainService;
  private Project project;
  private ProjectSettings settings;

  @BeforeEach
  void setUp() {
    domainService = new EndUserRegistrationDomainService();

    Organization org = Organization.create("Test Org", Slug.of("test-org"));
    project = Project.create(org, "Test Project", Slug.of("test-project"));
    settings = project.getSettings();
  }

  @Test
  void prepareRegistrationReturnsValidAttempt() {
    RegistrationAttempt attempt = domainService.prepareRegistration(
        project,
        settings,
        Email.of("user@example.com"),
        "ValidPass123",
        "Test User");

    assertThat(attempt.project()).isEqualTo(project);
    assertThat(attempt.email().value()).isEqualTo("user@example.com");
    assertThat(attempt.validatedPassword().value()).isEqualTo("ValidPass123");
    assertThat(attempt.validatedPassword().hashed()).isFalse();
    assertThat(attempt.name()).isEqualTo("Test User");
    assertThat(attempt.settings()).isEqualTo(settings);
  }

  @Test
  void prepareRegistrationThrowsWhenSettingsIsNull() {
    assertThatThrownBy(() ->
        domainService.prepareRegistration(
            project,
            null,
            Email.of("user@example.com"),
            "ValidPass123",
            "Test User"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("project_settings_missing");
  }

  @Test
  void prepareRegistrationValidatesPasswordPolicy() {
    // Default settings: minLength=8, requireUppercase=true, requireLowercase=true,
    // requireNumbers=true, requireSpecialChars=false

    assertThatThrownBy(() ->
        domainService.prepareRegistration(
            project,
            settings,
            Email.of("user@example.com"),
            "short1",  // too short
            "Test User"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("password_too_short");

    assertThatThrownBy(() ->
        domainService.prepareRegistration(
            project,
            settings,
            Email.of("user@example.com"),
            "lowercase123",  // missing uppercase
            "Test User"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("password_missing_uppercase");

    assertThatThrownBy(() ->
        domainService.prepareRegistration(
            project,
            settings,
            Email.of("user@example.com"),
            "UPPERCASE123",  // missing lowercase
            "Test User"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("password_missing_lowercase");

    assertThatThrownBy(() ->
        domainService.prepareRegistration(
            project,
            settings,
            Email.of("user@example.com"),
            "NoNumbersHere",  // missing numbers
            "Test User"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("password_missing_number");
  }

  @Test
  void prepareRegistrationWithSpecialCharPolicy() {
    // Update settings to require special characters
    settings.updatePasswordPolicy(
        8,  // minLength
        true,  // requireUppercase
        true,  // requireLowercase
        true,  // requireNumbers
        true,  // requireSpecialChars
        0  // passwordHistoryCount
    );

    assertThatThrownBy(() ->
        domainService.prepareRegistration(
            project,
            settings,
            Email.of("user@example.com"),
            "NoSpecialChar123",  // missing special char
            "Test User"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("password_missing_special_char");

    // Should succeed with special character
    RegistrationAttempt attempt = domainService.prepareRegistration(
        project,
        settings,
        Email.of("user@example.com"),
        "Valid@Pass123",
        "Test User");

    assertThat(attempt.validatedPassword().value()).isEqualTo("Valid@Pass123");
  }

  @ParameterizedTest
  @CsvSource({
      "ValidPass1, 8",           // exactly minimum length
      "LongPassword123, 15",     // longer than minimum
      "Short7A, 7"               // custom short minimum
  })
  void prepareRegistrationRespectsMinLength(String password, int minLength) {
    settings.updatePasswordPolicy(
        minLength,
        true,   // requireUppercase
        true,   // requireLowercase
        true,   // requireNumbers
        false,  // requireSpecialChars
        0       // passwordHistoryCount
    );

    RegistrationAttempt attempt = domainService.prepareRegistration(
        project,
        settings,
        Email.of("user@example.com"),
        password,
        "Test User");

    assertThat(attempt.validatedPassword().value()).isEqualTo(password);
  }

  @Test
  void prepareRegistrationWithNullName() {
    RegistrationAttempt attempt = domainService.prepareRegistration(
        project,
        settings,
        Email.of("user@example.com"),
        "ValidPass123",
        null);

    assertThat(attempt.name()).isNull();
  }

  @Test
  void prepareRegistrationWithEmptyName() {
    RegistrationAttempt attempt = domainService.prepareRegistration(
        project,
        settings,
        Email.of("user@example.com"),
        "ValidPass123",
        "");

    assertThat(attempt.name()).isEmpty();
  }
}
