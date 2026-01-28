package dev.auctoritas.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
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
public class AppleOAuthAuthorizeGatewayFilterFactory
    extends AbstractGatewayFilterFactory<AppleOAuthAuthorizeGatewayFilterFactory.Config> {
  private static final String CALLBACK_PATH = "/api/v1/auth/oauth/apple/callback";
  private static final String PROJECT_ID_HEADER = "X-Project-Id";
  private static final String AUTHORIZE_URL = "https://appleid.apple.com/auth/authorize";
  private static final String SCOPE = "email";

  private static final int STATE_BYTES = 32;
  private static final int CODE_VERIFIER_BYTES = 48;

  private final WebClient webClient;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public AppleOAuthAuthorizeGatewayFilterFactory(
      WebClient.Builder webClientBuilder,
      @Value("${AUTH_SERVICE_URL:http://localhost:8081}") String authServiceUrl) {
    super(Config.class);
    this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {

      String projectIdRaw = exchange.getRequest().getHeaders().getFirst(PROJECT_ID_HEADER);
      if (projectIdRaw == null || projectIdRaw.isBlank()) {
        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "api_key_invalid");
      }

      UUID projectId;
      try {
        projectId = UUID.fromString(projectIdRaw.trim());
      } catch (IllegalArgumentException e) {
        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "api_key_invalid");
      }

      String appRedirectUri = exchange.getRequest().getQueryParams().getFirst("redirect_uri");
      if (appRedirectUri == null || appRedirectUri.isBlank()) {
        return writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "oauth_redirect_uri_missing");
      }

      String state = generateToken(STATE_BYTES);
      String codeVerifier = generateToken(CODE_VERIFIER_BYTES);
      String codeChallenge = generateCodeChallenge(codeVerifier);
      String callbackUri =
          UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
              .replacePath(CALLBACK_PATH)
              .replaceQuery(null)
              .fragment(null)
              .build(true)
              .toUriString();

      InternalAppleAuthorizeRequest request =
          new InternalAppleAuthorizeRequest(projectId, appRedirectUri, state, codeVerifier);

      return webClient
          .post()
          .uri("/internal/oauth/apple/authorize")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchangeToMono(
              response -> {
                HttpStatus status = HttpStatus.resolve(response.statusCode().value());
                if (status != null && status.is2xxSuccessful()) {
                  return response
                      .bodyToMono(InternalAppleAuthorizeResponse.class)
                      .flatMap(
                          body -> {
                            if (body == null || body.clientId() == null || body.clientId().isBlank()) {
                              return writeErrorResponse(
                                  exchange, HttpStatus.BAD_GATEWAY, "oauth_apple_authorize_failed");
                            }
                            String appleAuthorizeUrl =
                                UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                                    .queryParam("client_id", body.clientId())
                                    .queryParam("redirect_uri", callbackUri)
                                    .queryParam("response_type", "code")
                                    .queryParam("response_mode", "query")
                                    .queryParam("scope", SCOPE)
                                    .queryParam("state", state)
                                    .queryParam("code_challenge", codeChallenge)
                                    .queryParam("code_challenge_method", "S256")
                                    .build()
                                    .encode()
                                    .toUriString();
                            return redirect(exchange, appleAuthorizeUrl);
                          });
                }

                HttpStatus resolved = status == null ? HttpStatus.BAD_GATEWAY : status;
                return response
                    .bodyToMono(ErrorResponse.class)
                    .defaultIfEmpty(new ErrorResponse("oauth_apple_authorize_failed"))
                    .flatMap(
                        err ->
                            writeErrorResponse(
                                exchange,
                                resolved,
                                err == null || err.error() == null || err.error().isBlank()
                                    ? "oauth_apple_authorize_failed"
                                    : err.error()));
              })
          .onErrorResume(
              WebClientRequestException.class,
              ex ->
                  writeErrorResponse(exchange, HttpStatus.BAD_GATEWAY, "oauth_apple_authorize_failed"))
          .onErrorResume(
              WebClientResponseException.class,
              ex ->
                  writeErrorResponse(exchange, HttpStatus.BAD_GATEWAY, "oauth_apple_authorize_failed"))
          .onErrorResume(
              ex -> writeErrorResponse(exchange, HttpStatus.BAD_GATEWAY, "oauth_apple_authorize_failed"));
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

  private String generateToken(int lengthBytes) {
    byte[] buffer = new byte[lengthBytes];
    secureRandom.nextBytes(buffer);
    return encoder.encodeToString(buffer);
  }

  private String generateCodeChallenge(String codeVerifier) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
      return encoder.encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
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

  private record InternalAppleAuthorizeRequest(
      UUID projectId, String redirectUri, String state, String codeVerifier) {}

  private record InternalAppleAuthorizeResponse(String clientId) {}

  private record ErrorResponse(String error) {}

  public static class Config {}
}
