package dev.auctoritas.auth.infrastructure.messaging;

public class NoopDomainEventPublisher implements DomainEventPublisher {
  @Override
  public void publish(String eventType, Object payload) {
    // intentionally no-op
  }
}
