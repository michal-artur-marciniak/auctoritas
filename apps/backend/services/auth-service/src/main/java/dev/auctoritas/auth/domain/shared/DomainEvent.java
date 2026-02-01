package dev.auctoritas.auth.domain.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events.
 * Domain events represent something that happened in the domain that other parts may care about.
 */
public interface DomainEvent {
  
  /**
   * Unique identifier for this event occurrence.
   */
  UUID eventId();
  
  /**
   * When the event occurred.
   */
  Instant occurredAt();
  
  /**
   * The aggregate ID that generated this event.
   */
  UUID aggregateId();
  
  /**
   * The type of event (for routing/handling).
   */
  String eventType();
}
