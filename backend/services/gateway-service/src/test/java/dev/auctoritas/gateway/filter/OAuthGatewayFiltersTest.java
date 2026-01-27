package dev.auctoritas.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

class OAuthGatewayFiltersTest {
  private static final UUID PROJECT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final String API_KEY = "test-api-key";

  private static DisposableServer authServer;
  private static String authServiceUrl;

  private static final AtomicReference<String> lastAuthorizeBody = new AtomicReference<>();
  private static final AtomicReference<String> lastCallbackBody = new AtomicReference<>();
  private static final AtomicReference<String> lastGithubAuthorizeBody = new AtomicReference<>();
  private static final AtomicReference<String> lastGithubCallbackBody = new AtomicReference<>();
  private static final AtomicReference<String> lastMicrosoftAuthorizeBody = new AtomicReference<>();
  private static final AtomicReference<String> lastMicrosoftCallbackBody = new AtomicReference<>();
  private static final AtomicReference<String> lastFacebookAuthorizeBody = new AtomicReference<>();
  private static final AtomicReference<String> lastFacebookCallbackBody = new AtomicReference<>();
  private static final AtomicReference<String> lastAppleAuthorizeBody = new AtomicReference<>();
  private static final AtomicReference<String> lastAppleCallbackBody = new AtomicReference<>();

  @BeforeAll
  static void startAuthServer() {
    authServer =
        HttpServer.create()
            .host("127.0.0.1")
            .port(0)
            .route(
                routes -> {
                  routes.post(
                      "/internal/api-keys/resolve",
                      (req, res) -> {
                        String apiKey = req.requestHeaders().get("X-API-Key");
                        if (!API_KEY.equals(apiKey)) {
                          return res
                              .status(401)
                              .header("Content-Type", "application/json")
                              .sendString(Mono.just("{\"error\":\"api_key_invalid\"}"));
                        }
                        return res
                            .status(200)
                            .header("Content-Type", "application/json")
                            .sendString(Mono.just("{\"projectId\":\"" + PROJECT_ID + "\"}"));
                      });

                  routes.post(
                      "/internal/oauth/google/authorize",
                      (req, res) ->
                          req.receive()
                              .aggregate()
                              .asString()
                              .doOnNext(lastAuthorizeBody::set)
                              .then(
                                  res.status(200)
                                      .header("Content-Type", "application/json")
                                      .sendString(
                                          Mono.just("{\"clientId\":\"test-google-client-id\"}"))
                                      .then()));

                  routes.post(
                      "/internal/oauth/google/callback",
                      (req, res) ->
                          req.receive()
                              .aggregate()
                              .asString()
                              .doOnNext(lastCallbackBody::set)
                              .then(
                                  res.status(200)
                                      .header("Content-Type", "application/json")
                                      .sendString(
                                          Mono.just(
                                              "{\"redirectUrl\":\"https://app.example/after?auctoritas_code=abc\"}"))
                                      .then()));

                  routes.post(
                      "/internal/oauth/github/authorize",
                      (req, res) ->
                          req.receive()
                              .aggregate()
                              .asString()
                              .doOnNext(lastGithubAuthorizeBody::set)
                              .then(
                                  res.status(200)
                                      .header("Content-Type", "application/json")
                                      .sendString(
                                          Mono.just("{\"clientId\":\"test-github-client-id\"}"))
                                      .then()));

                  routes.post(
                      "/internal/oauth/github/callback",
                      (req, res) ->
                          req.receive()
                              .aggregate()
                              .asString()
                              .doOnNext(lastGithubCallbackBody::set)
                              .then(
                                  res.status(200)
                                      .header("Content-Type", "application/json")
                                      .sendString(
                                          Mono.just(
                                              "{\"redirectUrl\":\"https://app.example/after?auctoritas_code=def\"}"))
                                      .then()));

                  routes.post(
                      "/internal/oauth/microsoft/authorize",
                      (req, res) ->
                          req.receive()
                              .aggregate()
                              .asString()
                              .doOnNext(lastMicrosoftAuthorizeBody::set)
                              .then(
                                  res.status(200)
                                      .header("Content-Type", "application/json")
                                      .sendString(
                                          Mono.just(
                                              "{\"clientId\":\"test-microsoft-client-id\",\"authorizationEndpoint\":\"https://login.microsoftonline.com/common/oauth2/v2.0/authorize\",\"scope\":\"openid profile email User.Read\"}"))
                                      .then()));

                  routes.post(
                      "/internal/oauth/microsoft/callback",
                      (req, res) ->
                          req.receive()
                              .aggregate()
                              .asString()
                              .doOnNext(lastMicrosoftCallbackBody::set)
                              .then(
                                  res.status(200)
                                      .header("Content-Type", "application/json")
                                      .sendString(
                                          Mono.just(
                                              "{\"redirectUrl\":\"https://app.example/after?auctoritas_code=ghi\"}"))
                                      .then()));

                  routes.post(
                      "/internal/oauth/facebook/authorize",
                      (req, res) ->
                          req.receive()
                              .aggregate()
                              .asString()
                              .doOnNext(lastFacebookAuthorizeBody::set)
                              .then(
                                  res.status(200)
                                      .header("Content-Type", "application/json")
                                      .sendString(
                                          Mono.just("{\"clientId\":\"test-facebook-client-id\"}"))
                                      .then()));

                  routes.post(
                      "/internal/oauth/facebook/callback",
                      (req, res) ->
                          req.receive()
                              .aggregate()
                              .asString()
                              .doOnNext(lastFacebookCallbackBody::set)
                              .then(
                                  res.status(200)
                                      .header("Content-Type", "application/json")
                                      .sendString(
                                          Mono.just(
                                              "{\"redirectUrl\":\"https://app.example/after?auctoritas_code=jkl\"}"))
                                      .then()));

                  routes.post(
                      "/internal/oauth/apple/authorize",
                      (req, res) ->
                          req.receive()
                              .aggregate()
                              .asString()
                              .doOnNext(lastAppleAuthorizeBody::set)
                              .then(
                                  res.status(200)
                                      .header("Content-Type", "application/json")
                                      .sendString(
                                          Mono.just("{\"clientId\":\"test-apple-service-id\"}"))
                                      .then()));

                  routes.post(
                      "/internal/oauth/apple/callback",
                      (req, res) ->
                          req.receive()
                              .aggregate()
                              .asString()
                              .doOnNext(lastAppleCallbackBody::set)
                              .then(
                                  res.status(200)
                                      .header("Content-Type", "application/json")
                                      .sendString(
                                          Mono.just(
                                              "{\"redirectUrl\":\"https://app.example/after?auctoritas_code=mno\"}"))
                                      .then()));
                })
            .bindNow();

    authServiceUrl = "http://127.0.0.1:" + authServer.port();
  }

  @AfterAll
  static void stopAuthServer() {
    if (authServer != null) {
      authServer.disposeNow();
    }
  }

  @Test
  void authorize_missingApiKey_returns401() {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get(
                    "http://example.test/api/v1/auth/oauth/google/authorize?redirect_uri=https://app.example/redirect")
                .build());

    GatewayFilterChain chain = ex -> Mono.error(new AssertionError("chain should not be called"));

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }

  @Test
  void authorize_withApiKey_redirectsToGoogle_andUsesPkce() throws Exception {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);
    GoogleOAuthAuthorizeGatewayFilterFactory authorizeFactory =
        new GoogleOAuthAuthorizeGatewayFilterFactory(WebClient.builder(), authServiceUrl);
    GatewayFilter authorizeFilter =
        authorizeFactory.apply(new GoogleOAuthAuthorizeGatewayFilterFactory.Config());

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("http://example.test/api/v1/auth/oauth/google/authorize")
                .header("X-API-Key", API_KEY)
                .queryParam("redirect_uri", "https://app.example/redirect")
                .build());

    GatewayFilterChain chain = ex -> authorizeFilter.filter(ex, e -> Mono.empty());

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.FOUND, exchange.getResponse().getStatusCode());
    String location = exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION);
    assertNotNull(location);
    assertTrue(location.startsWith("https://accounts.google.com/o/oauth2/v2/auth"));

    UriComponents uri = UriComponentsBuilder.fromUriString(location).build(true);
    assertEquals("test-google-client-id", uri.getQueryParams().getFirst("client_id"));
    assertEquals("code", uri.getQueryParams().getFirst("response_type"));
    assertEquals(
        "openid email profile",
        URLDecoder.decode(uri.getQueryParams().getFirst("scope"), StandardCharsets.UTF_8));
    assertEquals("S256", uri.getQueryParams().getFirst("code_challenge_method"));
    assertEquals(
        "http://example.test/api/v1/auth/oauth/google/callback",
        uri.getQueryParams().getFirst("redirect_uri"));

    String state = uri.getQueryParams().getFirst("state");
    String codeChallenge = uri.getQueryParams().getFirst("code_challenge");
    assertNotNull(state);
    assertTrue(!state.isBlank());
    assertNotNull(codeChallenge);
    assertTrue(!codeChallenge.isBlank());

    String bodyJson = lastAuthorizeBody.get();
    assertNotNull(bodyJson);
    assertEquals(PROJECT_ID.toString(), jsonField(bodyJson, "projectId"));
    assertEquals("https://app.example/redirect", jsonField(bodyJson, "redirectUri"));
    assertEquals(state, jsonField(bodyJson, "state"));

    String codeVerifier = jsonField(bodyJson, "codeVerifier");
    assertTrue(!codeVerifier.isBlank());
    assertEquals(codeChallenge, computeCodeChallenge(codeVerifier));
  }

  @Test
  void callback_withoutApiKey_isAllowed_andRedirects() throws Exception {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);
    GoogleOAuthCallbackGatewayFilterFactory callbackFactory =
        new GoogleOAuthCallbackGatewayFilterFactory(WebClient.builder(), authServiceUrl);
    GatewayFilter callbackFilter = callbackFactory.apply(new GoogleOAuthCallbackGatewayFilterFactory.Config());

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("http://example.test/api/v1/auth/oauth/google/callback")
                .queryParam("code", "google-code")
                .queryParam("state", "oauth-state")
                .build());

    GatewayFilterChain chain = ex -> callbackFilter.filter(ex, e -> Mono.empty());

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.FOUND, exchange.getResponse().getStatusCode());
    assertEquals(
        "https://app.example/after?auctoritas_code=abc",
        exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION));

    String bodyJson = lastCallbackBody.get();
    assertNotNull(bodyJson);
    assertEquals("google-code", jsonField(bodyJson, "code"));
    assertEquals("oauth-state", jsonField(bodyJson, "state"));
    assertEquals("http://example.test/api/v1/auth/oauth/google/callback", jsonField(bodyJson, "callbackUri"));
  }

  @Test
  void github_authorize_withApiKey_redirectsToGitHub_andUsesPkce() throws Exception {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);
    GitHubOAuthAuthorizeGatewayFilterFactory authorizeFactory =
        new GitHubOAuthAuthorizeGatewayFilterFactory(WebClient.builder(), authServiceUrl);
    GatewayFilter authorizeFilter = authorizeFactory.apply(new GitHubOAuthAuthorizeGatewayFilterFactory.Config());

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("http://example.test/api/v1/auth/oauth/github/authorize")
                .header("X-API-Key", API_KEY)
                .queryParam("redirect_uri", "https://app.example/redirect")
                .build());

    GatewayFilterChain chain = ex -> authorizeFilter.filter(ex, e -> Mono.empty());

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.FOUND, exchange.getResponse().getStatusCode());
    String location = exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION);
    assertNotNull(location);
    assertTrue(location.startsWith("https://github.com/login/oauth/authorize"));

    UriComponents uri = UriComponentsBuilder.fromUriString(location).build(true);
    assertEquals("test-github-client-id", uri.getQueryParams().getFirst("client_id"));
    assertEquals(
        "read:user user:email",
        URLDecoder.decode(uri.getQueryParams().getFirst("scope"), StandardCharsets.UTF_8));
    assertEquals("S256", uri.getQueryParams().getFirst("code_challenge_method"));
    assertEquals(
        "http://example.test/api/v1/auth/oauth/github/callback",
        uri.getQueryParams().getFirst("redirect_uri"));

    String state = uri.getQueryParams().getFirst("state");
    String codeChallenge = uri.getQueryParams().getFirst("code_challenge");
    assertNotNull(state);
    assertTrue(!state.isBlank());
    assertNotNull(codeChallenge);
    assertTrue(!codeChallenge.isBlank());

    String bodyJson = lastGithubAuthorizeBody.get();
    assertNotNull(bodyJson);
    assertEquals(PROJECT_ID.toString(), jsonField(bodyJson, "projectId"));
    assertEquals("https://app.example/redirect", jsonField(bodyJson, "redirectUri"));
    assertEquals(state, jsonField(bodyJson, "state"));

    String codeVerifier = jsonField(bodyJson, "codeVerifier");
    assertTrue(!codeVerifier.isBlank());
    assertEquals(codeChallenge, computeCodeChallenge(codeVerifier));
  }

  @Test
  void github_callback_withoutApiKey_isAllowed_andRedirects() throws Exception {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);
    GitHubOAuthCallbackGatewayFilterFactory callbackFactory =
        new GitHubOAuthCallbackGatewayFilterFactory(WebClient.builder(), authServiceUrl);
    GatewayFilter callbackFilter = callbackFactory.apply(new GitHubOAuthCallbackGatewayFilterFactory.Config());

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("http://example.test/api/v1/auth/oauth/github/callback")
                .queryParam("code", "github-code")
                .queryParam("state", "oauth-state")
                .build());

    GatewayFilterChain chain = ex -> callbackFilter.filter(ex, e -> Mono.empty());

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.FOUND, exchange.getResponse().getStatusCode());
    assertEquals(
        "https://app.example/after?auctoritas_code=def",
        exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION));

    String bodyJson = lastGithubCallbackBody.get();
    assertNotNull(bodyJson);
    assertEquals("github-code", jsonField(bodyJson, "code"));
    assertEquals("oauth-state", jsonField(bodyJson, "state"));
    assertEquals(
        "http://example.test/api/v1/auth/oauth/github/callback",
        jsonField(bodyJson, "callbackUri"));
  }

  @Test
  void microsoft_authorize_withApiKey_redirectsToMicrosoft_andUsesPkce() throws Exception {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);
    MicrosoftOAuthAuthorizeGatewayFilterFactory authorizeFactory =
        new MicrosoftOAuthAuthorizeGatewayFilterFactory(WebClient.builder(), authServiceUrl);
    GatewayFilter authorizeFilter =
        authorizeFactory.apply(new MicrosoftOAuthAuthorizeGatewayFilterFactory.Config());

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("http://example.test/api/v1/auth/oauth/microsoft/authorize")
                .header("X-API-Key", API_KEY)
                .queryParam("redirect_uri", "https://app.example/redirect")
                .build());

    GatewayFilterChain chain = ex -> authorizeFilter.filter(ex, e -> Mono.empty());

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.FOUND, exchange.getResponse().getStatusCode());
    String location = exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION);
    assertNotNull(location);
    assertTrue(location.startsWith("https://login.microsoftonline.com/common/oauth2/v2.0/authorize"));

    UriComponents uri = UriComponentsBuilder.fromUriString(location).build(true);
    assertEquals("test-microsoft-client-id", uri.getQueryParams().getFirst("client_id"));
    assertEquals("code", uri.getQueryParams().getFirst("response_type"));
    assertEquals(
        "openid profile email User.Read",
        URLDecoder.decode(uri.getQueryParams().getFirst("scope"), StandardCharsets.UTF_8));
    assertEquals("S256", uri.getQueryParams().getFirst("code_challenge_method"));
    assertEquals(
        "http://example.test/api/v1/auth/oauth/microsoft/callback",
        uri.getQueryParams().getFirst("redirect_uri"));

    String state = uri.getQueryParams().getFirst("state");
    String codeChallenge = uri.getQueryParams().getFirst("code_challenge");
    assertNotNull(state);
    assertTrue(!state.isBlank());
    assertNotNull(codeChallenge);
    assertTrue(!codeChallenge.isBlank());

    String bodyJson = lastMicrosoftAuthorizeBody.get();
    assertNotNull(bodyJson);
    assertEquals(PROJECT_ID.toString(), jsonField(bodyJson, "projectId"));
    assertEquals("https://app.example/redirect", jsonField(bodyJson, "redirectUri"));
    assertEquals(state, jsonField(bodyJson, "state"));

    String codeVerifier = jsonField(bodyJson, "codeVerifier");
    assertTrue(!codeVerifier.isBlank());
    assertEquals(codeChallenge, computeCodeChallenge(codeVerifier));
  }

  @Test
  void microsoft_callback_withoutApiKey_isAllowed_andRedirects() throws Exception {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);
    MicrosoftOAuthCallbackGatewayFilterFactory callbackFactory =
        new MicrosoftOAuthCallbackGatewayFilterFactory(WebClient.builder(), authServiceUrl);
    GatewayFilter callbackFilter =
        callbackFactory.apply(new MicrosoftOAuthCallbackGatewayFilterFactory.Config());

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("http://example.test/api/v1/auth/oauth/microsoft/callback")
                .queryParam("code", "microsoft-code")
                .queryParam("state", "oauth-state")
                .build());

    GatewayFilterChain chain = ex -> callbackFilter.filter(ex, e -> Mono.empty());

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.FOUND, exchange.getResponse().getStatusCode());
    assertEquals(
        "https://app.example/after?auctoritas_code=ghi",
        exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION));

    String bodyJson = lastMicrosoftCallbackBody.get();
    assertNotNull(bodyJson);
    assertEquals("microsoft-code", jsonField(bodyJson, "code"));
    assertEquals("oauth-state", jsonField(bodyJson, "state"));
    assertEquals(
        "http://example.test/api/v1/auth/oauth/microsoft/callback",
        jsonField(bodyJson, "callbackUri"));
  }

  @Test
  void facebook_authorize_withApiKey_redirectsToFacebook_andUsesPkce() throws Exception {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);
    FacebookOAuthAuthorizeGatewayFilterFactory authorizeFactory =
        new FacebookOAuthAuthorizeGatewayFilterFactory(WebClient.builder(), authServiceUrl);
    GatewayFilter authorizeFilter =
        authorizeFactory.apply(new FacebookOAuthAuthorizeGatewayFilterFactory.Config());

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("http://example.test/api/v1/auth/oauth/facebook/authorize")
                .header("X-API-Key", API_KEY)
                .queryParam("redirect_uri", "https://app.example/redirect")
                .build());

    GatewayFilterChain chain = ex -> authorizeFilter.filter(ex, e -> Mono.empty());

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.FOUND, exchange.getResponse().getStatusCode());
    String location = exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION);
    assertNotNull(location);
    assertTrue(location.startsWith("https://www.facebook.com/v18.0/dialog/oauth"));

    UriComponents uri = UriComponentsBuilder.fromUriString(location).build(true);
    assertEquals("test-facebook-client-id", uri.getQueryParams().getFirst("client_id"));
    assertEquals("code", uri.getQueryParams().getFirst("response_type"));
    assertEquals(
        "email public_profile",
        URLDecoder.decode(uri.getQueryParams().getFirst("scope"), StandardCharsets.UTF_8));
    assertEquals("S256", uri.getQueryParams().getFirst("code_challenge_method"));
    assertEquals(
        "http://example.test/api/v1/auth/oauth/facebook/callback",
        uri.getQueryParams().getFirst("redirect_uri"));

    String state = uri.getQueryParams().getFirst("state");
    String codeChallenge = uri.getQueryParams().getFirst("code_challenge");
    assertNotNull(state);
    assertTrue(!state.isBlank());
    assertNotNull(codeChallenge);
    assertTrue(!codeChallenge.isBlank());

    String bodyJson = lastFacebookAuthorizeBody.get();
    assertNotNull(bodyJson);
    assertEquals(PROJECT_ID.toString(), jsonField(bodyJson, "projectId"));
    assertEquals("https://app.example/redirect", jsonField(bodyJson, "redirectUri"));
    assertEquals(state, jsonField(bodyJson, "state"));

    String codeVerifier = jsonField(bodyJson, "codeVerifier");
    assertTrue(!codeVerifier.isBlank());
    assertEquals(codeChallenge, computeCodeChallenge(codeVerifier));
  }

  @Test
  void facebook_callback_withoutApiKey_isAllowed_andRedirects() throws Exception {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);
    FacebookOAuthCallbackGatewayFilterFactory callbackFactory =
        new FacebookOAuthCallbackGatewayFilterFactory(WebClient.builder(), authServiceUrl);
    GatewayFilter callbackFilter =
        callbackFactory.apply(new FacebookOAuthCallbackGatewayFilterFactory.Config());

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("http://example.test/api/v1/auth/oauth/facebook/callback")
                .queryParam("code", "facebook-code")
                .queryParam("state", "oauth-state")
                .build());

    GatewayFilterChain chain = ex -> callbackFilter.filter(ex, e -> Mono.empty());

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.FOUND, exchange.getResponse().getStatusCode());
    assertEquals(
        "https://app.example/after?auctoritas_code=jkl",
        exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION));

    String bodyJson = lastFacebookCallbackBody.get();
    assertNotNull(bodyJson);
    assertEquals("facebook-code", jsonField(bodyJson, "code"));
    assertEquals("oauth-state", jsonField(bodyJson, "state"));
    assertEquals(
        "http://example.test/api/v1/auth/oauth/facebook/callback", jsonField(bodyJson, "callbackUri"));
  }

  @Test
  void apple_authorize_withApiKey_redirectsToApple_andUsesPkce() throws Exception {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);
    AppleOAuthAuthorizeGatewayFilterFactory authorizeFactory =
        new AppleOAuthAuthorizeGatewayFilterFactory(WebClient.builder(), authServiceUrl);
    GatewayFilter authorizeFilter = authorizeFactory.apply(new AppleOAuthAuthorizeGatewayFilterFactory.Config());

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("http://example.test/api/v1/auth/oauth/apple/authorize")
                .header("X-API-Key", API_KEY)
                .queryParam("redirect_uri", "https://app.example/redirect")
                .build());

    GatewayFilterChain chain = ex -> authorizeFilter.filter(ex, e -> Mono.empty());

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.FOUND, exchange.getResponse().getStatusCode());
    String location = exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION);
    assertNotNull(location);
    assertTrue(location.startsWith("https://appleid.apple.com/auth/authorize"));

    UriComponents uri = UriComponentsBuilder.fromUriString(location).build(true);
    assertEquals("test-apple-service-id", uri.getQueryParams().getFirst("client_id"));
    assertEquals("code", uri.getQueryParams().getFirst("response_type"));
    assertEquals("query", uri.getQueryParams().getFirst("response_mode"));
    assertEquals(
        "name email",
        URLDecoder.decode(uri.getQueryParams().getFirst("scope"), StandardCharsets.UTF_8));
    assertEquals("S256", uri.getQueryParams().getFirst("code_challenge_method"));
    assertEquals(
        "http://example.test/api/v1/auth/oauth/apple/callback",
        uri.getQueryParams().getFirst("redirect_uri"));

    String state = uri.getQueryParams().getFirst("state");
    String codeChallenge = uri.getQueryParams().getFirst("code_challenge");
    assertNotNull(state);
    assertTrue(!state.isBlank());
    assertNotNull(codeChallenge);
    assertTrue(!codeChallenge.isBlank());

    String bodyJson = lastAppleAuthorizeBody.get();
    assertNotNull(bodyJson);
    assertEquals(PROJECT_ID.toString(), jsonField(bodyJson, "projectId"));
    assertEquals("https://app.example/redirect", jsonField(bodyJson, "redirectUri"));
    assertEquals(state, jsonField(bodyJson, "state"));

    String codeVerifier = jsonField(bodyJson, "codeVerifier");
    assertTrue(!codeVerifier.isBlank());
    assertEquals(codeChallenge, computeCodeChallenge(codeVerifier));
  }

  @Test
  void apple_callback_withoutApiKey_isAllowed_andRedirects() throws Exception {
    ApiKeyGatewayFilter apiKeyFilter = new ApiKeyGatewayFilter(WebClient.builder(), authServiceUrl);
    AppleOAuthCallbackGatewayFilterFactory callbackFactory =
        new AppleOAuthCallbackGatewayFilterFactory(WebClient.builder(), authServiceUrl);
    GatewayFilter callbackFilter = callbackFactory.apply(new AppleOAuthCallbackGatewayFilterFactory.Config());

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("http://example.test/api/v1/auth/oauth/apple/callback")
                .queryParam("code", "apple-code")
                .queryParam("state", "oauth-state")
                .build());

    GatewayFilterChain chain = ex -> callbackFilter.filter(ex, e -> Mono.empty());

    apiKeyFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.FOUND, exchange.getResponse().getStatusCode());
    assertEquals(
        "https://app.example/after?auctoritas_code=mno",
        exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION));

    String bodyJson = lastAppleCallbackBody.get();
    assertNotNull(bodyJson);
    assertEquals("apple-code", jsonField(bodyJson, "code"));
    assertEquals("oauth-state", jsonField(bodyJson, "state"));
    assertEquals(
        "http://example.test/api/v1/auth/oauth/apple/callback",
        jsonField(bodyJson, "callbackUri"));
  }

  private static String computeCodeChallenge(String codeVerifier) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
  }

  private static String jsonField(String json, String field) {
    Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"");
    Matcher m = p.matcher(json);
    assertTrue(m.find(), "Missing JSON field: " + field + " in: " + json);
    return m.group(1);
  }
}
