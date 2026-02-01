package dev.auctoritas.auth.adapter.out.messaging;

public interface DomainEventPublisher {
  void publish(String eventType, Object payload);
}
