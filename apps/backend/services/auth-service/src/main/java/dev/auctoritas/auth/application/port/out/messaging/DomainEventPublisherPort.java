package dev.auctoritas.auth.application.port.out.messaging;

/**
 * Port for publishing domain events from the auth domain.
 */
public interface DomainEventPublisherPort {
  void publish(String eventType, Object payload);
}
