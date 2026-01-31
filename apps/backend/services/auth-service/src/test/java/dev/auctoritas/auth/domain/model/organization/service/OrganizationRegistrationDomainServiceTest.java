package dev.auctoritas.auth.domain.model.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.enduser.Email;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.organization.OrganizationMemberRole;
import dev.auctoritas.auth.domain.model.organization.OrganizationRegistrationDomainService;
import dev.auctoritas.auth.domain.model.project.Slug;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for OrganizationRegistrationDomainService.
 *
 * These are pure unit tests with no Spring context - testing domain logic only.
 */
class OrganizationRegistrationDomainServiceTest {

  private OrganizationRegistrationDomainService domainService;

  @BeforeEach
  void setUp() {
    domainService = new OrganizationRegistrationDomainService();
  }

  @Test
  void registerCreatesOrganizationWithOwner() {
    var result = domainService.register(
        "Acme Corp",
        "acme-corp",
        "owner@acme.com",
        "SecurePass123!",
        "John Doe");

    // Verify organization
    Organization org = result.organization();
    assertThat(org.getName()).isEqualTo("Acme Corp");
    assertThat(org.getSlug()).isEqualTo("acme-corp");
    assertThat(org.getMembers()).isEmpty(); // Member not added yet - application layer does this

    // Verify owner spec
    var ownerSpec = result.ownerSpec();
    assertThat(ownerSpec.email().value()).isEqualTo("owner@acme.com");
    assertThat(ownerSpec.plainPassword()).isEqualTo("SecurePass123!");
    assertThat(ownerSpec.name()).isEqualTo("John Doe");
    assertThat(ownerSpec.role()).isEqualTo(OrganizationMemberRole.OWNER);
  }

  @Test
  void registerNormalizesSlug() {
    var result = domainService.register(
        "Acme Corp",
        "  Acme-CORP  ",
        "owner@acme.com",
        "password123",
        null);

    assertThat(result.organization().getSlug()).isEqualTo("acme-corp");
  }

  @Test
  void registerNormalizesEmail() {
    var result = domainService.register(
        "Acme Corp",
        "acme",
        "  Owner@ACME.COM  ",
        "password123",
        null);

    assertThat(result.ownerSpec().email().value()).isEqualTo("owner@acme.com");
  }

  @Test
  void registerWithNullName() {
    var result = domainService.register(
        "Acme Corp",
        "acme",
        "owner@acme.com",
        "password123",
        null);

    assertThat(result.ownerSpec().name()).isNull();
  }

  @Test
  void registerWithEmptyName() {
    var result = domainService.register(
        "Acme Corp",
        "acme",
        "owner@acme.com",
        "password123",
        "   ");

    assertThat(result.ownerSpec().name()).isNull();
  }

  @Test
  void registerThrowsWhenOrgNameIsNull() {
    assertThatThrownBy(() ->
        domainService.register(
            null,
            "acme",
            "owner@acme.com",
            "password123",
            null))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("org_name_required");
  }

  @Test
  void registerThrowsWhenOrgNameIsEmpty() {
    assertThatThrownBy(() ->
        domainService.register(
            "   ",
            "acme",
            "owner@acme.com",
            "password123",
            null))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("org_name_required");
  }

  @Test
  void registerThrowsWhenSlugIsNull() {
    assertThatThrownBy(() ->
        domainService.register(
            "Acme Corp",
            null,
            "owner@acme.com",
            "password123",
            null))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("org_slug_required");
  }

  @Test
  void registerThrowsWhenSlugIsEmpty() {
    assertThatThrownBy(() ->
        domainService.register(
            "Acme Corp",
            "   ",
            "owner@acme.com",
            "password123",
            null))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("org_slug_required");
  }

  @Test
  void registerThrowsWhenOwnerEmailIsNull() {
    assertThatThrownBy(() ->
        domainService.register(
            "Acme Corp",
            "acme",
            null,
            "password123",
            null))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("owner_email_required");
  }

  @Test
  void registerThrowsWhenOwnerEmailIsEmpty() {
    assertThatThrownBy(() ->
        domainService.register(
            "Acme Corp",
            "acme",
            "   ",
            "password123",
            null))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("owner_email_required");
  }

  @Test
  void registerThrowsWhenOwnerPasswordIsNull() {
    assertThatThrownBy(() ->
        domainService.register(
            "Acme Corp",
            "acme",
            "owner@acme.com",
            null,
            null))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("owner_password_required");
  }

  @Test
  void registerThrowsWhenOwnerPasswordIsEmpty() {
    assertThatThrownBy(() ->
        domainService.register(
            "Acme Corp",
            "acme",
            "owner@acme.com",
            "   ",
            null))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("owner_password_required");
  }

  @Test
  void registerThrowsWhenSlugIsInvalid() {
    assertThatThrownBy(() ->
        domainService.register(
            "Acme Corp",
            "invalid slug with spaces!",
            "owner@acme.com",
            "password123",
            null))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void registerThrowsWhenEmailIsInvalid() {
    assertThatThrownBy(() ->
        domainService.register(
            "Acme Corp",
            "acme",
            "not-an-email",
            "password123",
            null))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void ownerSpecValidatesRoleMustBeOwner() {
    assertThatThrownBy(() ->
        new OrganizationRegistrationDomainService.OwnerSpec(
            Email.of("test@test.com"),
            "password",
            null,
            OrganizationMemberRole.ADMIN))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("initial_member_must_be_owner");
  }

  @Test
  void ownerSpecValidatesNonNullFields() {
    var email = Email.of("test@test.com");

    assertThatThrownBy(() ->
        new OrganizationRegistrationDomainService.OwnerSpec(
            null, "password", null, OrganizationMemberRole.OWNER))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("email cannot be null");

    assertThatThrownBy(() ->
        new OrganizationRegistrationDomainService.OwnerSpec(
            email, null, null, OrganizationMemberRole.OWNER))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("plainPassword cannot be null");

    assertThatThrownBy(() ->
        new OrganizationRegistrationDomainService.OwnerSpec(
            email, "password", null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("role cannot be null");
  }

  @Test
  void registrationResultValidatesNonNullFields() {
    Organization org = Organization.create("Test", Slug.of("test"));
    var ownerSpec = new OrganizationRegistrationDomainService.OwnerSpec(
        Email.of("test@test.com"),
        "password",
        null,
        OrganizationMemberRole.OWNER);

    assertThatThrownBy(() ->
        new OrganizationRegistrationDomainService.RegistrationResult(null, ownerSpec))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("organization cannot be null");

    assertThatThrownBy(() ->
        new OrganizationRegistrationDomainService.RegistrationResult(org, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("ownerSpec cannot be null");
  }
}
