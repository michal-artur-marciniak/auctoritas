package dev.auctoritas.auth.domain.model.enduser;

import dev.auctoritas.auth.domain.event.DomainEvent;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.valueobject.Email;
import dev.auctoritas.auth.domain.valueobject.Password;
import dev.auctoritas.auth.shared.persistence.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * Aggregate root representing an end-user in the identity domain.
 */
@Entity
@Table(
    name = "end_users",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "email"}))
@Getter
public class EndUser extends BaseAuditEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(length = 100)
  private String name;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts = 0;

  @Column(name = "failed_login_window_start")
  private Instant failedLoginWindowStart;

  @Column(name = "lockout_until")
  private Instant lockoutUntil;

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  protected EndUser() {
  }

  /**
   * Factory method to create a new EndUser with validated data.
   * Publishes UserRegisteredEvent.
   */
  public static EndUser create(Project project, Email email, Password password, String name) {
    EndUser user = new EndUser();
    user.project = project;
    user.email = email.value();
    user.setPassword(password);
    user.name = normalizeName(name);
    user.emailVerified = false;
    user.failedLoginAttempts = 0;
    
    // Register domain event
    user.registerEvent(new UserRegisteredEvent(
        user.getId(),
        project.getId(),
        user.email,
        user.name,
        user.emailVerified,
        Instant.now()
    ));
    
    return user;
  }

  private static String normalizeName(String name) {
    if (name == null) {
      return null;
    }
    String trimmed = name.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public void setPassword(Password password) {
    if (password == null) {
      throw new DomainValidationException("password_required");
    }
    if (!password.isHashed()) {
      throw new DomainValidationException("password_must_be_hashed");
    }
    this.passwordHash = password.value();
  }

  /**
   * Updates the user's name.
   */
  public void updateName(String newName) {
    this.name = normalizeName(newName);
  }

  /**
   * Verifies the user's email address.
   * Publishes EmailVerifiedEvent.
   */
  public void verifyEmail() {
    if (this.emailVerified) {
      return; // Already verified, no event
    }
    this.emailVerified = true;
    
    registerEvent(new EmailVerifiedEvent(
        UUID.randomUUID(),
        getId(),
        this.email,
        Instant.now()
    ));
  }

  /**
   * Records a failed login attempt and potentially locks the account.
   *
   * @param maxAttempts maximum allowed failed attempts
   * @param windowSeconds time window for counting attempts
   * @param now current time
   * @return true if account is now locked
   */
  public boolean recordFailedLogin(int maxAttempts, int windowSeconds, Instant now) {
    resetExpiredFailedAttempts(windowSeconds, now);

    if (failedLoginWindowStart == null) {
      failedLoginWindowStart = now;
      failedLoginAttempts = 1;
    } else {
      failedLoginAttempts++;
    }

    if (failedLoginAttempts >= maxAttempts) {
      lockoutUntil = now.plusSeconds(windowSeconds);
      
      registerEvent(new AccountLockedEvent(
          UUID.randomUUID(),
          getId(),
          this.email,
          lockoutUntil,
          Instant.now()
      ));
      
      return true;
    }
    return false;
  }

  /**
   * Clears failed login attempts after successful login.
   */
  public void clearFailedAttempts() {
    this.failedLoginAttempts = 0;
    this.failedLoginWindowStart = null;
    this.lockoutUntil = null;
  }

  /**
   * Checks if the account is currently locked.
   */
  public boolean isLocked(Instant now) {
    if (lockoutUntil == null) {
      return false;
    }
    if (lockoutUntil.isAfter(now)) {
      return true;
    }
    lockoutUntil = null;
    return false;
  }

  private void resetExpiredFailedAttempts(int windowSeconds, Instant now) {
    if (lockoutUntil != null && !lockoutUntil.isAfter(now)) {
      lockoutUntil = null;
    }

    if (failedLoginWindowStart != null) {
      Instant windowEnd = failedLoginWindowStart.plusSeconds(windowSeconds);
      if (windowEnd.isBefore(now)) {
        failedLoginAttempts = 0;
        failedLoginWindowStart = null;
      }
    }
  }

  /**
   * Validates that the user can attempt login (not locked, email verified if required).
   */
  public void validateCanLogin(boolean requireVerifiedEmail, Instant now) {
    if (isLocked(now)) {
      throw new DomainValidationException("account_locked");
    }
    if (requireVerifiedEmail && !emailVerified) {
      throw new DomainValidationException("email_not_verified");
    }
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
   * Clears all domain events. Should be called after events are published.
   */
  public void clearDomainEvents() {
    domainEvents.clear();
  }
}
