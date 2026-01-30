package dev.auctoritas.auth.adapters.external.oauth;

import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.ports.oauth.OAuthProviderPort;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeDetails;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeUrlRequest;
import dev.auctoritas.auth.service.oauth.OAuthTokenExchangeRequest;
import dev.auctoritas.auth.service.oauth.OAuthUserInfo;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleOAuthProvider implements OAuthProviderPort {
  private static final String PROVIDER = "google";
  private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
  private static final String SCOPE = "openid email profile";

  private final TextEncryptor oauthClientSecretEncryptor;
  private final GoogleOAuthClient googleOAuthClient;

  public GoogleOAuthProvider(
      TextEncryptor oauthClientSecretEncryptor, GoogleOAuthClient googleOAuthClient) {
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
    this.googleOAuthClient = googleOAuthClient;
  }

  @Override
  public String name() {
    return PROVIDER;
  }

  @Override
  public OAuthAuthorizeDetails getAuthorizeDetails(ProjectSettings settings) {
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }
    Map<String, Object> oauthConfig = settings.getOauthConfig() == null ? Map.of() : settings.getOauthConfig();
    Object googleObj = oauthConfig.get(PROVIDER);
    if (!(googleObj instanceof Map<?, ?> googleRaw)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_not_configured");
    }

    boolean enabled = Boolean.TRUE.equals(googleRaw.get("enabled"));
    String clientId = trimToNull(googleRaw.get("clientId"));

    if (!enabled || clientId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_not_configured");
    }

    return new OAuthAuthorizeDetails(clientId, AUTHORIZE_URL, SCOPE);
  }

  @Override
  public String buildAuthorizeUrl(OAuthAuthorizeDetails details, OAuthAuthorizeUrlRequest request) {
    if (details == null || details.clientId() == null || details.clientId().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_not_configured");
    }
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_authorize_failed");
    }

    return UriComponentsBuilder.fromUriString(details.authorizationEndpoint())
        .queryParam("client_id", details.clientId())
        .queryParam("redirect_uri", request.callbackUri())
        .queryParam("response_type", "code")
        .queryParam("scope", details.scope())
        .queryParam("state", request.state())
        .queryParam("code_challenge", request.codeChallenge())
        .queryParam("code_challenge_method", request.codeChallengeMethod())
        .build()
        .encode()
        .toUriString();
  }

  @Override
  public OAuthUserInfo exchangeAuthorizationCode(ProjectSettings settings, OAuthTokenExchangeRequest request) {
    OAuthAuthorizeDetails details = getAuthorizeDetails(settings);

    String clientSecret = decryptClientSecret(settings);
    if (clientSecret == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_not_configured");
    }

    String code = requireValue(request == null ? null : request.code(), "oauth_code_missing");
    String callbackUri = requireValue(request == null ? null : request.callbackUri(), "oauth_callback_uri_missing");
    String codeVerifier = requireValue(request == null ? null : request.codeVerifier(), "oauth_code_verifier_missing");

    GoogleOAuthClient.GoogleTokenResponse tokenResponse =
        googleOAuthClient.exchangeAuthorizationCode(
            new GoogleOAuthClient.GoogleTokenExchangeRequest(
                code, details.clientId(), clientSecret, callbackUri, codeVerifier));

    GoogleOAuthClient.GoogleUserInfo userInfo =
        googleOAuthClient.fetchUserInfo(tokenResponse.accessToken());

    String providerUserId = requireValue(userInfo == null ? null : userInfo.sub(), "oauth_google_userinfo_failed");
    String email = requireValue(userInfo == null ? null : userInfo.email(), "oauth_google_userinfo_failed");
    Boolean emailVerified = userInfo == null ? null : userInfo.emailVerified();
    String name = userInfo == null ? null : userInfo.name();

    return new OAuthUserInfo(providerUserId, email, emailVerified, name);
  }

  private String decryptClientSecret(ProjectSettings settings) {
    String enc = settings.getOauthGoogleClientSecretEnc();
    String decrypted = enc == null || enc.trim().isEmpty() ? null : oauthClientSecretEncryptor.decrypt(enc);
    return decrypted == null ? null : trimToNull(decrypted);
  }

  private static String trimToNull(Object value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.toString().trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    return trimmed;
  }
}
