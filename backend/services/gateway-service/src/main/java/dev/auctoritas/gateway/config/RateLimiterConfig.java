package dev.auctoritas.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Configuration for rate limiting in the API Gateway.
 * Uses Redis-based rate limiting with IP address as the key.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Key resolver that extracts client IP address for rate limiting.
     * Falls back to "unknown" if IP cannot be determined.
     */
  @Bean
  public KeyResolver ipKeyResolver() {
    return exchange -> {
      String ip = exchange.getRequest().getRemoteAddress() != null
          ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
          : "unknown";
      return Mono.just(ip);
    };
  }

  /**
   * Key resolver that uses X-API-Key when present, otherwise falls back to IP.
   */
  @Bean
  public KeyResolver apiKeyResolver() {
    return exchange -> {
      String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
      if (apiKey != null && !apiKey.isBlank()) {
        return Mono.just(apiKey.trim());
      }
      String ip = exchange.getRequest().getRemoteAddress() != null
          ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
          : "unknown";
      return Mono.just(ip);
    };
  }
}
