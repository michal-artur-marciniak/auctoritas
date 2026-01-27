package dev.auctoritas.gateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
public class GitHubOAuthCallbackGatewayFilterFactory
    extends AbstractGatewayFilterFactory<GitHubOAuthCallbackGatewayFilterFactory.Config> {
  private static final String CALLBACK_PATH = "/api/v1/auth/oauth/github/callback";

  private final WebClient webClient;

  public GitHubOAuthCallbackGatewayFilterFactory(
      WebClient.Builder webClientBuilder,
      @Value("${AUTH_SERVICE_URL:http://localhost:8081}") String authServiceUrl) {
    super(Config.class);
    this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      String code = exchange.getRequest().getQueryParams().getFirst("code");
      if (code == null || code.isBlank()) {
        return writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "oauth_code_missing");
      }
      String state = exchange.getRequest().getQueryParams().getFirst("state");
      if (state == null || state.isBlank()) {
        return writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "oauth_state_missing");
      }

      String callbackUri =
          UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
              .replacePath(CALLBACK_PATH)
              .replaceQuery(null)
              .fragment(null)
              .build(true)
              .toUriString();

      InternalGitHubCallbackRequest request =
          new InternalGitHubCallbackRequest(code.trim(), state.trim(), callbackUri);

      return webClient
          .post()
          .uri("/internal/oauth/github/callback")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchangeToMono(
              response -> {
                HttpStatus status = HttpStatus.resolve(response.statusCode().value());
                if (status != null && status.is2xxSuccessful()) {
                  return response
                      .bodyToMono(InternalGitHubCallbackResponse.class)
                      .flatMap(
                          body -> {
                            if (body == null || body.redirectUrl() == null || body.redirectUrl().isBlank()) {
                              return writeErrorResponse(
                                  exchange, HttpStatus.BAD_GATEWAY, "oauth_github_callback_failed");
                            }
                            return redirect(exchange, body.redirectUrl());
                          });
                }

                HttpStatus resolved = status == null ? HttpStatus.BAD_GATEWAY : status;
                return response
                    .bodyToMono(ErrorResponse.class)
                    .defaultIfEmpty(new ErrorResponse("oauth_github_callback_failed"))
                    .flatMap(
                        err ->
                            writeErrorResponse(
                                exchange,
                                resolved,
                                err == null || err.error() == null || err.error().isBlank()
                                    ? "oauth_github_callback_failed"
                                    : err.error()));
              })
          .onErrorResume(
              WebClientRequestException.class,
              ex -> writeErrorResponse(exchange, HttpStatus.BAD_GATEWAY, "oauth_github_callback_failed"))
          .onErrorResume(
              WebClientResponseException.class,
              ex -> writeErrorResponse(exchange, HttpStatus.BAD_GATEWAY, "oauth_github_callback_failed"))
          .onErrorResume(
              ex -> writeErrorResponse(exchange, HttpStatus.BAD_GATEWAY, "oauth_github_callback_failed"));
    };
  }

  private Mono<Void> redirect(ServerWebExchange exchange, String location) {
    ServerHttpResponse response = exchange.getResponse();
    if (response.isCommitted()) {
      return Mono.empty();
    }
    response.setStatusCode(HttpStatus.FOUND);
    response.getHeaders().set(HttpHeaders.LOCATION, location);
    return response.setComplete();
  }

  private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String errorCode) {
    ServerHttpResponse response = exchange.getResponse();
    if (response.isCommitted()) {
      return Mono.empty();
    }
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] body = ("{\"error\":\"" + errorCode + "\"}").getBytes(StandardCharsets.UTF_8);
    return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
  }

  private record InternalGitHubCallbackRequest(String code, String state, String callbackUri) {}

  private record InternalGitHubCallbackResponse(String redirectUrl) {}

  private record ErrorResponse(String error) {}

  public static class Config {}
}
