package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.domain.event.DomainEvent;
import dev.auctoritas.auth.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Rich domain entity representing an organization member refresh token.
 * Refresh tokens are used to obtain new access tokens without requiring re-authentication.
 */
@Entity
@Table(name = "org_member_refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationMemberRefreshToken extends BaseEntity {

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private OrganizationMember member;

  @Column(name = "token_hash", nullable = false, length = 128)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean revoked = false;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "replaced_by_id")
  private OrganizationMemberRefreshToken replacedBy;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", length = 500)
  private String userAgent;

  /**
   * Creates a new refresh token for the specified organization member.
   *
   * @param member the organization member
   * @param tokenHash the hash of the token value
   * @param ttl the time-to-live duration
   * @param ipAddress the IP address of the client
   * @param userAgent the user agent string
   * @return a new OrganizationMemberRefreshToken instance
   * @throws IllegalArgumentException if member, tokenHash, or ttl is null
   */
  public static OrganizationMemberRefreshToken create(
      OrganizationMember member,
      String tokenHash,
      Duration ttl,
      String ipAddress,
      String userAgent) {
    Objects.requireNonNull(member, "member_required");
    Objects.requireNonNull(tokenHash, "token_hash_required");
    Objects.requireNonNull(ttl, "ttl_required");

    OrganizationMemberRefreshToken token = new OrganizationMemberRefreshToken();
    token.member = member;
    token.tokenHash = tokenHash;
    token.expiresAt = Instant.now().plus(ttl);
    token.revoked = false;
    token.ipAddress = ipAddress;
    token.userAgent = userAgent;

    token.registerEvent(
        new OrganizationMemberRefreshTokenCreatedEvent(
            UUID.randomUUID(),
            token.getId(),
            member.getId(),
            token.expiresAt,
            Instant.now()));

    return token;
  }

  /**
   * Rotates this token by creating a new token and marking this one as revoked.
   * This is used during token refresh to implement secure token rotation.
   *
   * @param newTokenHash the hash of the new token value
   * @param ttl the time-to-live for the new token
   * @param ipAddress the IP address of the client
   * @param userAgent the user agent string
   * @return the new refresh token
   * @throws IllegalStateException if this token is already revoked or expired
   * @throws IllegalArgumentException if newTokenHash or ttl is null
   */
  public OrganizationMemberRefreshToken rotate(
      String newTokenHash,
      Duration ttl,
      String ipAddress,
      String userAgent) {
    Objects.requireNonNull(newTokenHash, "new_token_hash_required");
    Objects.requireNonNull(ttl, "ttl_required");
    
    validateNotRevoked();
    validateNotExpired();

    // Create new token
    OrganizationMemberRefreshToken newToken = create(member, newTokenHash, ttl, ipAddress, userAgent);
    
    // Mark this token as replaced and revoked
    this.replacedBy = newToken;
    this.revoked = true;

    registerEvent(
        new OrganizationMemberRefreshTokenRotatedEvent(
            UUID.randomUUID(),
            getId(),
            member.getId(),
            newToken.getId(),
            Instant.now()));

    return newToken;
  }

  /**
   * Revokes this token.
   *
   * @param reason the reason for revocation
   * @throws IllegalStateException if token is already revoked
   */
  public void revoke(String reason) {
    validateNotRevoked();
    
    this.revoked = true;

    registerEvent(
        new OrganizationMemberRefreshTokenRevokedEvent(
            UUID.randomUUID(),
            getId(),
            member.getId(),
            reason != null ? reason : "manual_revocation",
            Instant.now()));
  }

  /**
   * Checks if the token has expired.
   *
   * @return true if current time is after expiresAt
   */
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  /**
   * Checks if the token is valid for use (not revoked and not expired).
   *
   * @return true if token can be used for refresh
   */
  public boolean isValidForRefresh() {
    return !revoked && !isExpired();
  }

  /**
   * Validates that the token belongs to the given member.
   *
   * @param memberId the member ID to check
   * @return true if token belongs to member
   */
  public boolean belongsTo(UUID memberId) {
    return member != null && member.getId().equals(memberId);
  }

  /**
   * Returns an unmodifiable view of domain events.
   *
   * @return list of domain events
   */
  public List<DomainEvent> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
  }

  /**
   * Clears all domain events. Should be called after events are published.
   */
  public void clearDomainEvents() {
    domainEvents.clear();
  }

  private void registerEvent(DomainEvent event) {
    domainEvents.add(event);
  }

  private void validateNotRevoked() {
    if (revoked) {
      throw new IllegalStateException("token_already_revoked");
    }
  }

  private void validateNotExpired() {
    if (isExpired()) {
      throw new IllegalStateException("token_expired");
    }
  }
}
