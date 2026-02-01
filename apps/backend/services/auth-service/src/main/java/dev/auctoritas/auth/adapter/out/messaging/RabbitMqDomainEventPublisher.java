package dev.auctoritas.auth.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitMqDomainEventPublisher implements DomainEventPublisher {
  private static final Logger log = LoggerFactory.getLogger(RabbitMqDomainEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public RabbitMqDomainEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(String eventType, Object payload) {
    if (eventType == null || eventType.isBlank()) {
      throw new IllegalArgumentException("eventType_required");
    }
    try {
      String body = objectMapper.writeValueAsString(payload);
      rabbitTemplate.convertAndSend("", eventType, body);
    } catch (JsonProcessingException ex) {
      log.warn("Failed to serialize domain event type={}", eventType, ex);
      throw new IllegalStateException("domain_event_serialization_failed", ex);
    } catch (AmqpException ex) {
      log.warn("Failed to publish domain event type={}", eventType, ex);
    }
  }
}
