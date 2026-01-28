package dev.auctoritas.gateway.filter;

import java.util.UUID;
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

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
    if (requestId == null || requestId.trim().isEmpty()) {
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
}
