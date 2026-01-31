package dev.auctoritas.auth.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
  @Bean
  @ConditionalOnProperty(
      value = "auctoritas.messaging.rabbit.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public DomainEventPublisher rabbitDomainEventPublisher(
      RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
    return new RabbitMqDomainEventPublisher(rabbitTemplate, objectMapper);
  }

  @Bean
  @ConditionalOnProperty(value = "auctoritas.messaging.rabbit.enabled", havingValue = "false")
  public DomainEventPublisher noopDomainEventPublisher() {
    return new NoopDomainEventPublisher();
  }
}
