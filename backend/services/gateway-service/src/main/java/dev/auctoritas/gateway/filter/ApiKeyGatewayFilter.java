package dev.auctoritas.gateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyGatewayFilter implements GlobalFilter, Ordered {
  private static final String API_KEY_HEADER = "X-API-Key";
  private static final String PROJECT_ID_HEADER = "X-Project-Id";
  private static final String SDK_AUTH_PREFIX = "/api/v1/auth/";
  private static final String SDK_USERS_PREFIX = "/api/v1/users/";
  private static final String GOOGLE_OAUTH_CALLBACK_PATH = "/api/v1/auth/oauth/google/callback";
  private static final String GITHUB_OAUTH_CALLBACK_PATH = "/api/v1/auth/oauth/github/callback";
  private static final String MICROSOFT_OAUTH_CALLBACK_PATH = "/api/v1/auth/oauth/microsoft/callback";
  private static final String FACEBOOK_OAUTH_CALLBACK_PATH = "/api/v1/auth/oauth/facebook/callback";

  private final WebClient webClient;

  public ApiKeyGatewayFilter(
      WebClient.Builder webClientBuilder,
      @Value("${AUTH_SERVICE_URL:http://localhost:8081}") String authServiceUrl) {
    this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    if (GOOGLE_OAUTH_CALLBACK_PATH.equals(path)
        || GITHUB_OAUTH_CALLBACK_PATH.equals(path)
        || MICROSOFT_OAUTH_CALLBACK_PATH.equals(path)
        || FACEBOOK_OAUTH_CALLBACK_PATH.equals(path)) {
      return chain.filter(exchange);
    }
    if (!isSdkPath(path)) {
      return chain.filter(exchange);
    }

    String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
    if (apiKey == null || apiKey.isBlank()) {
      return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "api_key_missing");
    }

    return webClient
        .post()
        .uri("/internal/api-keys/resolve")
        .header(API_KEY_HEADER, apiKey)
        .retrieve()
        .bodyToMono(ApiKeyResolutionResponse.class)
        .flatMap(
            response -> {
              if (response == null || response.projectId() == null) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "api_key_invalid");
              }
              ServerHttpRequest mutatedRequest =
                  exchange
                      .getRequest()
                      .mutate()
                      .header(PROJECT_ID_HEADER, response.projectId().toString())
                      .build();
              return chain.filter(exchange.mutate().request(mutatedRequest).build());
            })
        .onErrorResume(
            WebClientResponseException.class,
            ex -> {
              int statusCode = ex.getStatusCode().value();
              if (statusCode >= 400 && statusCode < 500) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "api_key_invalid");
              }
              return writeErrorResponse(exchange, HttpStatus.BAD_GATEWAY, "api_key_validation_failed");
            })
        .onErrorResume(
            WebClientRequestException.class,
            ex -> writeErrorResponse(exchange, HttpStatus.BAD_GATEWAY, "api_key_validation_failed"));
  }

  @Override
  public int getOrder() {
    return -100;
  }

  private boolean isSdkPath(String path) {
    return path.startsWith(SDK_AUTH_PREFIX) || path.startsWith(SDK_USERS_PREFIX);
  }

  private Mono<Void> writeErrorResponse(
      ServerWebExchange exchange, HttpStatus status, String errorCode) {
    ServerHttpResponse response = exchange.getResponse();
    if (response.isCommitted()) {
      return Mono.empty();
    }
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] body = ("{\"error\":\"" + errorCode + "\"}").getBytes(StandardCharsets.UTF_8);
    return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
  }
}
