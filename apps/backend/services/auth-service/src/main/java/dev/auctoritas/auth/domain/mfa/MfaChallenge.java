package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.shared.DomainEvent;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * Aggregate root for short-lived MFA challenge sessions during login.
 * Supports both end users and organization members.
 * Enforces single-use and expiration invariants.
 */
@Entity
@Table(name = "mfa_challenges")
@Getter
public class MfaChallenge extends BaseEntity {

  @Column(nullable = false, unique = true, length = 255)
  private String token;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private EndUser user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private OrganizationMember member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private Boolean used = false;

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  protected MfaChallenge() {
  }

  /**
   * Factory method to create an MFA challenge for an end user.
   *
   * @param user the end user
   * @param project the project
   * @param token the challenge token
   * @param expiresAt when the challenge expires
   * @return new MfaChallenge
   * @throws DomainValidationException if parameters are invalid
   */
  public static MfaChallenge createForUser(
      EndUser user,
      Project project,
      String token,
      Instant expiresAt) {

    validateCreateParams(user, project, token, expiresAt);

    MfaChallenge challenge = new MfaChallenge();
    challenge.user = user;
    challenge.project = project;
    challenge.token = token;
    challenge.expiresAt = expiresAt;
    challenge.used = false;

    challenge.registerEvent(new MfaChallengeInitiatedEvent(
        UUID.randomUUID(),
        challenge.getId(),
        user.getId(),
        project.getId(),
        challenge.getId(),
        expiresAt,
        Instant.now()
    ));

    return challenge;
  }

  /**
   * Factory method to create an MFA challenge for an organization member.
   *
   * @param member the organization member
   * @param organization the organization
   * @param token the challenge token
   * @param expiresAt when the challenge expires
   * @return new MfaChallenge
   * @throws DomainValidationException if parameters are invalid
   */
  public static MfaChallenge createForMember(
      OrganizationMember member,
      Organization organization,
      String token,
      Instant expiresAt) {

    if (member == null) {
      throw new DomainValidationException("member_required");
    }
    if (organization == null) {
      throw new DomainValidationException("organization_required");
    }
    if (token == null || token.isEmpty()) {
      throw new DomainValidationException("token_required");
    }
    if (expiresAt == null) {
      throw new DomainValidationException("expires_at_required");
    }
    if (expiresAt.isBefore(Instant.now())) {
      throw new DomainValidationException("expires_at_must_be_future");
    }

    MfaChallenge challenge = new MfaChallenge();
    challenge.member = member;
    challenge.organization = organization;
    challenge.token = token;
    challenge.expiresAt = expiresAt;
    challenge.used = false;

    challenge.registerEvent(new MfaChallengeInitiatedEvent(
        UUID.randomUUID(),
        challenge.getId(),
        member.getId(),
        organization.getId(),
        challenge.getId(),
        expiresAt,
        Instant.now()
    ));

    return challenge;
  }

  private static void validateCreateParams(
      EndUser user,
      Project project,
      String token,
      Instant expiresAt) {

    if (user == null) {
      throw new DomainValidationException("user_required");
    }
    if (project == null) {
      throw new DomainValidationException("project_required");
    }
    if (token == null || token.isEmpty()) {
      throw new DomainValidationException("token_required");
    }
    if (expiresAt == null) {
      throw new DomainValidationException("expires_at_required");
    }
    if (expiresAt.isBefore(Instant.now())) {
      throw new DomainValidationException("expires_at_must_be_future");
    }
  }

  /**
   * Marks the challenge as used.
   * Can only be used once and must not be expired.
   *
   * @throws DomainValidationException if already used or expired
   */
  public void markUsed() {
    if (Boolean.TRUE.equals(this.used)) {
      throw new DomainValidationException("challenge_already_used");
    }
    if (isExpired(Instant.now())) {
      throw new DomainValidationException("challenge_expired");
    }

    this.used = true;

    // Determine IDs based on user or member
    UUID userId = user != null ? user.getId() : member.getId();
    UUID projectOrOrgId = project != null ? project.getId() : organization.getId();

    registerEvent(new MfaChallengeCompletedEvent(
        UUID.randomUUID(),
        this.getId(),
        userId,
        projectOrOrgId,
        this.getId(),
        "TOTP",
        Instant.now()
    ));
  }

  /**
   * Checks if the challenge has expired.
   */
  public boolean isExpired(Instant now) {
    return expiresAt.isBefore(now);
  }

  /**
   * Checks if the challenge is valid (not used and not expired).
   */
  public boolean isValid(Instant now) {
    return !Boolean.TRUE.equals(this.used) && !isExpired(now);
  }

  /**
   * Returns the user ID if this is a user challenge, null otherwise.
   */
  public UUID getUserId() {
    return user != null ? user.getId() : null;
  }

  /**
   * Returns the member ID if this is a member challenge, null otherwise.
   */
  public UUID getMemberId() {
    return member != null ? member.getId() : null;
  }

  /**
   * Registers a domain event.
   */
  protected void registerEvent(DomainEvent event) {
    domainEvents.add(event);
  }

  /**
   * Returns unmodifiable list of domain events.
   */
  public List<DomainEvent> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
  }

  /**
   * Clears all domain events.
   */
  public void clearDomainEvents() {
    domainEvents.clear();
  }
}
