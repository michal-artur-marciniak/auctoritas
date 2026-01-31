package dev.auctoritas.auth.domain.model.organization.service;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.organization.OrgMemberRole;
import dev.auctoritas.auth.domain.valueobject.Email;
import dev.auctoritas.auth.domain.valueobject.Slug;
import java.util.Objects;

/**
 * Domain service for Organization registration operations.
 *
 * This is a pure domain service that encapsulates business logic for registering
 * new organizations with an owner member. It handles the cross-aggregate operation
 * of creating an Organization and its initial Owner member specification.
 *
 * <p>As a pure domain service, this class should NOT be annotated with Spring's @Service.
 * It should be instantiated directly by application services.
 *
 * Responsibilities:
 * - Validate registration input data
 * - Create Organization aggregate
 * - Create Owner specification (validated data for member creation)
 * - Enforce business invariants (organization must have owner)
 */
public class OrganizationRegistrationDomainService {

  /**
   * Represents the result of an organization registration attempt.
   * Contains the created organization and owner specification.
   */
  public record RegistrationResult(
      Organization organization,
      OwnerSpec ownerSpec) {

    public RegistrationResult {
      Objects.requireNonNull(organization, "organization cannot be null");
      Objects.requireNonNull(ownerSpec, "ownerSpec cannot be null");
    }
  }

  /**
   * Specification for creating an owner member.
   * Contains validated data - password hashing and member creation is handled by application layer.
   */
  public record OwnerSpec(
      Email email,
      String plainPassword,
      String name,
      OrgMemberRole role) {

    public OwnerSpec {
      Objects.requireNonNull(email, "email cannot be null");
      Objects.requireNonNull(plainPassword, "plainPassword cannot be null");
      Objects.requireNonNull(role, "role cannot be null");
      if (role != OrgMemberRole.OWNER) {
        throw new DomainValidationException("initial_member_must_be_owner");
      }
    }
  }

  /**
   * Registers a new organization with owner specification.
   *
   * <p>This method performs pure business logic:
   * - Validates input data
   * - Creates the Organization aggregate (publishes OrganizationCreatedEvent)
   * - Returns OwnerSpec with validated owner data
   *
   * <p>The application layer must:
   * - Check slug uniqueness before calling this method
   * - Hash the owner password
   * - Create OrganizationMember with hashed password
   * - Add member to organization
   * - Save the organization (cascades to member)
   * - Publish domain events
   * - Generate authentication tokens
   *
   * @param orgName organization name
   * @param slugValue organization slug (will be validated and normalized)
   * @param ownerEmail owner email address
   * @param ownerPassword owner plain-text password (will be validated, NOT hashed here)
   * @param ownerName owner display name (may be null)
   * @return RegistrationResult containing the created organization and owner specification
   * @throws DomainValidationException if validation fails
   */
  public RegistrationResult register(
      String orgName,
      String slugValue,
      String ownerEmail,
      String ownerPassword,
      String ownerName) {

    // Validate organization data
    if (orgName == null || orgName.trim().isEmpty()) {
      throw new DomainValidationException("org_name_required");
    }

    // Create slug value object (validates format)
    Slug slug = createSlug(slugValue);

    // Create owner specification (validates owner data)
    OwnerSpec ownerSpec = createOwnerSpec(ownerEmail, ownerPassword, ownerName);

    // Create organization aggregate (publishes OrganizationCreatedEvent)
    Organization organization = Organization.create(orgName.trim(), slug);

    return new RegistrationResult(organization, ownerSpec);
  }

  private Slug createSlug(String slugValue) {
    if (slugValue == null || slugValue.trim().isEmpty()) {
      throw new DomainValidationException("org_slug_required");
    }
    return Slug.of(slugValue.trim().toLowerCase());
  }

  private OwnerSpec createOwnerSpec(String email, String password, String name) {
    if (email == null || email.trim().isEmpty()) {
      throw new DomainValidationException("owner_email_required");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new DomainValidationException("owner_password_required");
    }

    Email validatedEmail = Email.of(email);
    String trimmedPassword = password.trim();
    String trimmedName = trimToNull(name);

    return new OwnerSpec(validatedEmail, trimmedPassword, trimmedName, OrgMemberRole.OWNER);
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
