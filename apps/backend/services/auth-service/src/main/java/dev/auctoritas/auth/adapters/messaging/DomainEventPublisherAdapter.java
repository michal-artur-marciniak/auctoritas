package dev.auctoritas.auth.adapters.messaging;

import dev.auctoritas.auth.messaging.DomainEventPublisher;
import dev.auctoritas.auth.ports.messaging.DomainEventPublisherPort;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link DomainEventPublisher} via {@link DomainEventPublisherPort}.
 */
@Component
public class DomainEventPublisherAdapter implements DomainEventPublisherPort {
  private final DomainEventPublisher domainEventPublisher;

  public DomainEventPublisherAdapter(DomainEventPublisher domainEventPublisher) {
    this.domainEventPublisher = domainEventPublisher;
  }

  @Override
  public void publish(String eventType, Object payload) {
    domainEventPublisher.publish(eventType, payload);
  }
}
