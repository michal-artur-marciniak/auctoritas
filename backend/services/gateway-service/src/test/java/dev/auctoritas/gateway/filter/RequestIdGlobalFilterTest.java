package dev.auctoritas.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RequestIdGlobalFilterTest {

  @Test
  void generatesRequestIdWhenMissing() {
    RequestIdGlobalFilter filter = new RequestIdGlobalFilter();
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/auth/login").build());

    AtomicReference<String> downstreamRequestId = new AtomicReference<>();
    GatewayFilterChain chain =
        ex -> {
          downstreamRequestId.set(
              ex.getRequest().getHeaders().getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER));
          return ex.getResponse().setComplete();
        };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(downstreamRequestId.get()).isNotNull();
    assertThat(downstreamRequestId.get().trim()).isNotEmpty();
    assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER))
        .isEqualTo(downstreamRequestId.get());
  }

  @Test
  void keepsRequestIdWhenProvided() {
    RequestIdGlobalFilter filter = new RequestIdGlobalFilter();
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/auth/login")
                .header(RequestIdGlobalFilter.REQUEST_ID_HEADER, "req-123")
                .build());

    AtomicReference<String> downstreamRequestId = new AtomicReference<>();
    GatewayFilterChain chain =
        ex -> {
          downstreamRequestId.set(
              ex.getRequest().getHeaders().getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER));
          return ex.getResponse().setComplete();
        };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(downstreamRequestId.get()).isEqualTo("req-123");
    assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER))
        .isEqualTo("req-123");
  }
}
