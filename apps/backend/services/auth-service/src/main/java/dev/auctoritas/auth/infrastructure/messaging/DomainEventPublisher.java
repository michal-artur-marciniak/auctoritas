package dev.auctoritas.auth.infrastructure.messaging;

public interface DomainEventPublisher {
  void publish(String eventType, Object payload);
}
