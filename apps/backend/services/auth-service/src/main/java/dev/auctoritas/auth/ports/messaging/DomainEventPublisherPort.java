package dev.auctoritas.auth.ports.messaging;

/**
 * Port for publishing domain events from the auth domain.
 */
public interface DomainEventPublisherPort {
  void publish(String eventType, Object payload);
}
