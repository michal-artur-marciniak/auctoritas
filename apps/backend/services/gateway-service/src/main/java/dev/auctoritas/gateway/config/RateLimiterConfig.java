package dev.auctoritas.gateway.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
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
  private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
  private static final String REAL_IP_HEADER = "X-Real-IP";

  private final List<String> trustedProxies;

  public RateLimiterConfig(@Value("${auth.security.trusted-proxies:}") List<String> trustedProxies) {
    this.trustedProxies = trustedProxies == null
        ? List.of()
        : trustedProxies.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toUnmodifiableList());
  }

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
    if (isFromTrustedProxy(exchange)) {
      String forwardedFor = exchange.getRequest().getHeaders().getFirst(FORWARDED_FOR_HEADER);
      String forwardedIp = extractForwardedFor(forwardedFor);
      if (forwardedIp != null) {
        return forwardedIp;
      }
      String realIp = exchange.getRequest().getHeaders().getFirst(REAL_IP_HEADER);
      if (realIp != null && !realIp.isBlank()) {
        return realIp.trim();
      }
    }
    return resolveRemoteAddress(exchange);
  }

  private boolean isFromTrustedProxy(ServerWebExchange exchange) {
    if (trustedProxies.isEmpty()) {
      return false;
    }
    InetSocketAddress remoteAddr = exchange.getRequest().getRemoteAddress();
    if (remoteAddr == null) {
      return false;
    }
    InetAddress address = remoteAddr.getAddress();
    String remoteIp = address != null ? address.getHostAddress() : remoteAddr.getHostString();
    if (remoteIp == null || remoteIp.isBlank()) {
      return false;
    }
    for (String proxy : trustedProxies) {
      if (proxy.equals(remoteIp)) {
        return true;
      }
      if (proxy.contains("/") && cidrMatches(remoteIp, proxy)) {
        return true;
      }
    }
    return false;
  }

  private String extractForwardedFor(String forwardedFor) {
    if (forwardedFor == null || forwardedFor.isBlank()) {
      return null;
    }
    for (String part : forwardedFor.split(",")) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        return trimmed;
      }
    }
    return null;
  }

  private String resolveRemoteAddress(ServerWebExchange exchange) {
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

  private boolean cidrMatches(String ip, String cidr) {
    int slashIndex = cidr.indexOf('/');
    if (slashIndex <= 0 || slashIndex == cidr.length() - 1) {
      return false;
    }
    String baseIp = cidr.substring(0, slashIndex).trim();
    String prefixPart = cidr.substring(slashIndex + 1).trim();
    int prefixLength;
    try {
      prefixLength = Integer.parseInt(prefixPart);
    } catch (NumberFormatException ex) {
      return false;
    }
    try {
      InetAddress address = InetAddress.getByName(ip);
      InetAddress network = InetAddress.getByName(baseIp);
      byte[] addressBytes = address.getAddress();
      byte[] networkBytes = network.getAddress();
      if (addressBytes.length != networkBytes.length) {
        return false;
      }
      int maxBits = addressBytes.length * 8;
      if (prefixLength < 0 || prefixLength > maxBits) {
        return false;
      }
      int fullBytes = prefixLength / 8;
      int remainingBits = prefixLength % 8;
      for (int i = 0; i < fullBytes; i++) {
        if (addressBytes[i] != networkBytes[i]) {
          return false;
        }
      }
      if (remainingBits == 0) {
        return true;
      }
      int mask = (0xFF << (8 - remainingBits)) & 0xFF;
      int addressByte = addressBytes[fullBytes] & 0xFF;
      int networkByte = networkBytes[fullBytes] & 0xFF;
      return (addressByte & mask) == (networkByte & mask);
    } catch (Exception ex) {
      return false;
    }
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
