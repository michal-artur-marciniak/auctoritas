package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a recovery code is used.
 */
public record RecoveryCodeUsedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    UUID projectId,
    String codeHash,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "mfa.recovery_code.used";
  }
}
