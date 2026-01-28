package dev.auctoritas.gateway.filter;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {
  public static final String REQUEST_ID_HEADER = "X-Request-Id";

  private static final int MAX_REQUEST_ID_LENGTH = 128;
  private static final Pattern SAFE_REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9-_]{0,127}$");
  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String requestId = sanitizeRequestId(exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER));
    if (requestId == null) {
      requestId = UUID.randomUUID().toString();
    }

    final String finalRequestId = requestId;

    ServerHttpRequest mutatedRequest =
        exchange
            .getRequest()
            .mutate()
            .headers(headers -> headers.set(REQUEST_ID_HEADER, finalRequestId))
            .build();

    exchange
        .getResponse()
        .beforeCommit(
            () -> {
              exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, finalRequestId);
              return Mono.empty();
            });

    return chain.filter(exchange.mutate().request(mutatedRequest).build());
  }

  private static String sanitizeRequestId(String raw) {
    if (raw == null) {
      return null;
    }
    String candidate = raw.trim();
    if (candidate.isEmpty() || candidate.length() > MAX_REQUEST_ID_LENGTH || containsControlChars(candidate)) {
      return null;
    }

    String lower = candidate.toLowerCase(Locale.ROOT);
    if (UUID_PATTERN.matcher(lower).matches()) {
      return candidate;
    }
    if (SAFE_REQUEST_ID_PATTERN.matcher(candidate).matches()) {
      return candidate;
    }
    return null;
  }

  private static boolean containsControlChars(String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\r' || c == '\n' || Character.isISOControl(c)) {
        return true;
      }
    }
    return false;
  }
}
