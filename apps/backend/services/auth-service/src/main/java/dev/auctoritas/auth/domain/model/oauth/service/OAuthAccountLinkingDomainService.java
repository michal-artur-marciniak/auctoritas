package dev.auctoritas.auth.domain.model.oauth.service;

import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.oauth.OAuthConnection;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.valueobject.Email;
import java.util.Locale;
import java.util.Optional;

/**
 * Domain service for OAuth account linking operations.
 *
 * This is a pure domain service that encapsulates business logic for linking
 * OAuth provider accounts to EndUsers. It handles the decision-making process
 * without interacting with infrastructure.
 *
 * <p>As a pure domain service, this class should NOT be annotated with Spring's @Service.
 * It should be instantiated directly by application services.
 *
 * Responsibilities:
 * - Determine linking strategy (existing connection, existing user, new user)
 * - Validate business rules (email verification requirements)
 * - Define user creation/update specifications
 * - Handle email reconciliation logic
 */
public class OAuthAccountLinkingDomainService {

  /**
   * Represents the result of an OAuth linking attempt.
   */
  public record LinkingResult(
      Type type,
      EndUser user,
      Optional<OAuthConnectionUpdate> connectionUpdate,
      Optional<EndUserUpdate> userUpdate) {

    public enum Type {
      EXISTING_CONNECTION,      // User linked via existing OAuth connection
      EXISTING_USER_LINKED,     // Linked to existing verified email user
      NEW_USER_CREATED          // Created new user for this OAuth account
    }
  }

  /**
   * Specification for updating an OAuth connection.
   */
  public record OAuthConnectionUpdate(
      OAuthConnection connection,
      String newEmail) {
  }

  /**
   * Specification for updating an EndUser.
   */
  public record EndUserUpdate(
      EndUser user,
      boolean verifyEmail,
      Optional<String> newName) {
  }

  /**
   * Evaluates OAuth linking strategy and returns specifications for updates.
   *
   * This method performs pure business logic to determine:
   * - Whether to use an existing connection
   * - Whether to link to an existing user
   * - Whether to create a new user
   *
   * The application layer must:
   * - Execute the actual database operations
   * - Handle race conditions and locking
   * - Manage transactions
   *
   * @param project the project context
   * @param provider OAuth provider identifier (e.g., "google", "github")
   * @param providerUserId unique user identifier from the provider
   * @param email email address from OAuth provider (may be null)
   * @param emailVerified whether the email is verified by the provider
   * @param name display name from OAuth provider (may be null)
   * @param existingConnection existing OAuth connection if found
   * @param existingVerifiedUser existing user with verified email if found
   * @return LinkingResult with specifications for updates
   * @throws DomainValidationException if required fields are missing
   * @throws DomainConflictException if email is unverified and conflicts with existing user
   */
  public LinkingResult evaluateLinking(
      Project project,
      String provider,
      String providerUserId,
      String email,
      Boolean emailVerified,
      String name,
      Optional<OAuthConnection> existingConnection,
      Optional<EndUser> existingVerifiedUser) {

    if (project == null) {
      throw new DomainValidationException("project_required");
    }

    String resolvedProvider = requireValue(provider, "oauth_provider_required");
    String resolvedProviderUserId = requireValue(providerUserId, "oauth_provider_user_id_required");

    String normalizedEmail = normalizeEmailOrNull(email);
    String normalizedName = trimToNull(name);
    boolean isEmailVerified = Boolean.TRUE.equals(emailVerified);

    // Case 1: Existing OAuth connection - update email if changed
    if (existingConnection.isPresent()) {
      OAuthConnection conn = existingConnection.get();
      Optional<OAuthConnectionUpdate> connectionUpdate = Optional.empty();

      if (normalizedEmail != null && !normalizedEmail.equals(conn.getEmail())) {
        connectionUpdate = Optional.of(new OAuthConnectionUpdate(conn, normalizedEmail));
      }

      EndUser user = conn.getUser();
      Optional<EndUserUpdate> userUpdate = determineUserUpdate(user, normalizedName, isEmailVerified);

      return new LinkingResult(
          LinkingResult.Type.EXISTING_CONNECTION,
          user,
          connectionUpdate,
          userUpdate);
    }

    // Require email for new connections
    String resolvedEmail = requireValue(normalizedEmail, "oauth_email_required");

    // Case 2: Email not verified and conflicts with existing user
    if (!isEmailVerified && existingVerifiedUser.isPresent()) {
      throw new DomainConflictException("oauth_email_unverified_conflict");
    }

    // Case 3: Verified email matches existing user - link to them
    if (isEmailVerified && existingVerifiedUser.isPresent()) {
      EndUser user = existingVerifiedUser.get();
      Optional<EndUserUpdate> userUpdate = determineUserUpdate(user, normalizedName, isEmailVerified);

      return new LinkingResult(
          LinkingResult.Type.EXISTING_USER_LINKED,
          user,
          Optional.empty(),
          userUpdate);
    }

    // Case 4: Create new user
    return new LinkingResult(
        LinkingResult.Type.NEW_USER_CREATED,
        null, // Will be created by application layer
        Optional.empty(),
        Optional.empty());
  }

  /**
   * Creates specifications for a new OAuth user.
   *
   * @param project the project
   * @param email normalized email address
   * @param name display name (may be null)
   * @param emailVerified whether email is verified
   * @return UserCreationSpec with all data needed to create the user
   */
  public UserCreationSpec createUserSpec(
      Project project,
      String email,
      String name,
      boolean emailVerified) {

    return new UserCreationSpec(
        project,
        Email.of(email),
        trimToNull(name),
        emailVerified);
  }

  /**
   * Specification for creating a new OAuth user.
   */
  public record UserCreationSpec(
      Project project,
      Email email,
      String name,
      boolean emailVerified) {
  }

  private Optional<EndUserUpdate> determineUserUpdate(
      EndUser user, String normalizedName, boolean isEmailVerified) {

    boolean shouldVerifyEmail = isEmailVerified && !user.isEmailVerified();
    boolean shouldUpdateName = user.getName() == null && normalizedName != null;

    if (!shouldVerifyEmail && !shouldUpdateName) {
      return Optional.empty();
    }

    return Optional.of(new EndUserUpdate(
        user,
        shouldVerifyEmail,
        shouldUpdateName ? Optional.of(normalizedName) : Optional.empty()));
  }

  private static String normalizeEmailOrNull(String email) {
    String trimmed = trimToNull(email);
    return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
  }

  private static String requireValue(String value, String errorCode) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
