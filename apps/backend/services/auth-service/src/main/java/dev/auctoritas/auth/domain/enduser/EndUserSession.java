package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.util.Objects;
import dev.auctoritas.auth.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Rich domain entity representing an end-user session.
 * Sessions track authenticated user sessions with expiration and device information.
 */
@Entity
@Table(name = "end_user_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EndUserSession extends BaseEntity {

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private EndUser user;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "device_info", columnDefinition = "jsonb")
  private Map<String, Object> deviceInfo;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "invalidated", nullable = false)
  private boolean invalidated = false;

  @Column(name = "invalidated_at")
  private Instant invalidatedAt;

  /**
   * Creates a new user session.
   *
   * @param user the authenticated user
   * @param ipAddress the IP address of the client
   * @param deviceInfo device information map
   * @param ttl time-to-live duration for the session
   * @return a new EndUserSession instance
   * @throws IllegalArgumentException if user is null or ttl is null
   */
  public static EndUserSession create(
      EndUser user, String ipAddress, Map<String, Object> deviceInfo, Duration ttl) {
    Objects.requireNonNull(user, "user_required");
    Objects.requireNonNull(ttl, "ttl_required");

    EndUserSession session = new EndUserSession();
    session.user = user;
    session.ipAddress = ipAddress;
    session.deviceInfo = deviceInfo != null ? Map.copyOf(deviceInfo) : null;
    session.expiresAt = Instant.now().plus(ttl);
    session.invalidated = false;

    session.registerEvent(
        new EndUserSessionCreatedEvent(
            UUID.randomUUID(),
            session.getId(),
            user.getId(),
            ipAddress,
            Instant.now()));

    return session;
  }

  /**
   * Extends the session expiration time.
   *
   * @param extension the duration to extend by
   * @throws IllegalStateException if session is invalidated
   * @throws IllegalArgumentException if extension is null
   */
  public void extend(Duration extension) {
    Objects.requireNonNull(extension, "extension_required");
    validateNotInvalidated();

    this.expiresAt = Instant.now().plus(extension);

    registerEvent(
        new EndUserSessionExtendedEvent(
            UUID.randomUUID(),
            getId(),
            user.getId(),
            this.expiresAt,
            Instant.now()));
  }

  /**
   * Updates device info and IP address for the session.
   *
   * @param ipAddress new IP address
   * @param deviceInfo new device info
   */
  public void updateDeviceInfo(String ipAddress, Map<String, Object> deviceInfo) {
    validateNotInvalidated();
    this.ipAddress = ipAddress;
    this.deviceInfo = deviceInfo != null ? Map.copyOf(deviceInfo) : null;
  }

  /**
   * Invalidates the session (logout).
   *
   * @param reason the reason for invalidation
   */
  public void invalidate(String reason) {
    if (this.invalidated) {
      return; // Already invalidated, idempotent
    }

    this.invalidated = true;
    this.invalidatedAt = Instant.now();

    registerEvent(
        new EndUserSessionInvalidatedEvent(
            UUID.randomUUID(),
            getId(),
            user.getId(),
            reason != null ? reason : "logout",
            Instant.now()));
  }

  /**
   * Checks if the session has expired.
   *
   * @return true if current time is after expiresAt
   */
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  /**
   * Checks if the session is valid (not expired and not invalidated).
   *
   * @return true if session is active and not expired
   */
  public boolean isValid() {
    return !invalidated && !isExpired();
  }

  /**
   * Validates that the session belongs to the given user.
   *
   * @param userId the user ID to check
   * @return true if session belongs to user
   */
  public boolean belongsTo(UUID userId) {
    return user != null && user.getId().equals(userId);
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

  private void validateNotInvalidated() {
    if (invalidated) {
      throw new IllegalStateException("session_invalidated");
    }
  }
}
