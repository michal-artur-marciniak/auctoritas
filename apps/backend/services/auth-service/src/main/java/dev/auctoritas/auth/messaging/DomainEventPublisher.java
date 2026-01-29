package dev.auctoritas.auth.messaging;

public interface DomainEventPublisher {
  void publish(String eventType, Object payload);
}
