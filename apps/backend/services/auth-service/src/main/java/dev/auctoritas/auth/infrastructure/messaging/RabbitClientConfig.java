package dev.auctoritas.auth.infrastructure.messaging;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@ConditionalOnProperty(
    value = "auctoritas.messaging.rabbit.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RabbitClientConfig {

  @Bean
  public ConnectionFactory rabbitConnectionFactory(
      @Value("${spring.rabbitmq.host:localhost}") String host,
      @Value("${spring.rabbitmq.port:5672}") int port,
      @Value("${spring.rabbitmq.username:guest}") String username,
      @Value("${spring.rabbitmq.password:guest}") String password,
      @Value("${spring.rabbitmq.virtual-host:/}") String virtualHost) {
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
    connectionFactory.setHost(host);
    connectionFactory.setPort(port);
    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);
    connectionFactory.setVirtualHost(virtualHost);
    return connectionFactory;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory rabbitConnectionFactory) {
    return new RabbitTemplate(rabbitConnectionFactory);
  }
}
