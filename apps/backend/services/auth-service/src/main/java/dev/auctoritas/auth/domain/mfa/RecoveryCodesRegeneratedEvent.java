package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when new recovery codes are generated.
 */
public record RecoveryCodesRegeneratedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    UUID projectId,
    int count,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "mfa.recovery_codes.regenerated";
  }
}
