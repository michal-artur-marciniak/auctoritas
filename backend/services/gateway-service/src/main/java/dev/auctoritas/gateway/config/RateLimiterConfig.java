package dev.auctoritas.gateway.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HexFormat;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
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
      String ip = resolveClientIp(exchange);
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
        return Mono.just(hashApiKey(apiKey.trim()));
      }
      String ip = resolveClientIp(exchange);
      return Mono.just(ip);
    };
  }

  private String resolveClientIp(ServerWebExchange exchange) {
    InetSocketAddress remoteAddr = exchange.getRequest().getRemoteAddress();
    if (remoteAddr == null) {
      return "unknown";
    }
    InetAddress address = remoteAddr.getAddress();
    if (address != null) {
      return address.getHostAddress();
    }
    String hostString = remoteAddr.getHostString();
    if (hostString != null && !hostString.isBlank()) {
      return hostString;
    }
    String fallback = remoteAddr.toString();
    if (fallback != null && !fallback.isBlank()) {
      return fallback;
    }
    return "unknown";
  }

  private String hashApiKey(String apiKey) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }
}
