package dev.auctoritas.auth.infrastructure.messaging;

import dev.auctoritas.auth.infrastructure.messaging.DomainEventPublisher;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
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
