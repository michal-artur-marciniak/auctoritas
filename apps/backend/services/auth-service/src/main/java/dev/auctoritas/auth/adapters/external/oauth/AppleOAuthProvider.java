package dev.auctoritas.auth.adapters.external.oauth;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.application.port.out.oauth.OAuthProviderPort;
import dev.auctoritas.auth.service.oauth.AppleClientSecretService;
import dev.auctoritas.auth.service.oauth.AppleIdTokenValidator;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeDetails;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeUrlRequest;
import dev.auctoritas.auth.service.oauth.OAuthTokenExchangeRequest;
import dev.auctoritas.auth.service.oauth.OAuthUserInfo;
import java.util.Map;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AppleOAuthProvider implements OAuthProviderPort {
  private static final String PROVIDER = "apple";
  private static final String AUTHORIZE_URL = "https://appleid.apple.com/auth/authorize";
  private static final String SCOPE = "email";

  private final TextEncryptor oauthClientSecretEncryptor;
  private final AppleOAuthClient appleOAuthClient;
  private final AppleClientSecretService appleClientSecretService;
  private final AppleIdTokenValidator appleIdTokenValidator;

  public AppleOAuthProvider(
      TextEncryptor oauthClientSecretEncryptor,
      AppleOAuthClient appleOAuthClient,
      AppleClientSecretService appleClientSecretService,
      AppleIdTokenValidator appleIdTokenValidator) {
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
    this.appleOAuthClient = appleOAuthClient;
    this.appleClientSecretService = appleClientSecretService;
    this.appleIdTokenValidator = appleIdTokenValidator;
  }

  @Override
  public String name() {
    return PROVIDER;
  }

  @Override
  public OAuthAuthorizeDetails getAuthorizeDetails(ProjectSettings settings) {
    if (settings == null) {
      throw new DomainValidationException("project_settings_missing");
    }
    Map<String, Object> oauthConfig =
        settings.getOauthConfig() == null ? Map.of() : settings.getOauthConfig();
    Object appleObj = oauthConfig.get(PROVIDER);
    if (!(appleObj instanceof Map<?, ?> appleRaw)) {
      throw new DomainValidationException("oauth_apple_not_configured");
    }

    boolean enabled = Boolean.TRUE.equals(appleRaw.get("enabled"));
    String serviceId = trimToNull(appleRaw.get("serviceId"));
    String teamId = trimToNull(appleRaw.get("teamId"));
    String keyId = trimToNull(appleRaw.get("keyId"));

    if (!enabled || serviceId == null || teamId == null || keyId == null) {
      throw new DomainValidationException("oauth_apple_not_configured");
    }

    return new OAuthAuthorizeDetails(serviceId, AUTHORIZE_URL, SCOPE);
  }

  @Override
  public String buildAuthorizeUrl(OAuthAuthorizeDetails details, OAuthAuthorizeUrlRequest request) {
    if (details == null || details.clientId() == null || details.clientId().isBlank()) {
      throw new DomainValidationException("oauth_apple_not_configured");
    }
    if (request == null) {
      throw new DomainValidationException("oauth_apple_authorize_failed");
    }

    return UriComponentsBuilder.fromUriString(details.authorizationEndpoint())
        .queryParam("client_id", details.clientId())
        .queryParam("redirect_uri", request.callbackUri())
        .queryParam("response_type", "code")
        .queryParam("response_mode", "query")
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

    Map<String, Object> oauthConfig =
        settings.getOauthConfig() == null ? Map.of() : settings.getOauthConfig();
    Object appleObj = oauthConfig.get(PROVIDER);
    if (!(appleObj instanceof Map<?, ?> appleRaw)) {
      throw new DomainValidationException("oauth_apple_not_configured");
    }

    String teamId = requireValue(trimToNull(appleRaw.get("teamId")), "oauth_apple_not_configured");
    String keyId = requireValue(trimToNull(appleRaw.get("keyId")), "oauth_apple_not_configured");
    String serviceId = requireValue(trimToNull(appleRaw.get("serviceId")), "oauth_apple_not_configured");

    String privateKeyPem = decryptPrivateKey(settings);
    if (privateKeyPem == null) {
      throw new DomainValidationException("oauth_apple_not_configured");
    }

    String code = requireValue(request == null ? null : request.code(), "oauth_code_missing");
    String callbackUri = requireValue(request == null ? null : request.callbackUri(), "oauth_callback_uri_missing");
    String codeVerifier = requireValue(request == null ? null : request.codeVerifier(), "oauth_code_verifier_missing");

    String clientSecretJwt =
        appleClientSecretService.createClientSecret(teamId, keyId, serviceId, privateKeyPem);

    AppleOAuthClient.AppleTokenResponse tokenResponse =
        appleOAuthClient.exchangeAuthorizationCode(
            new AppleOAuthClient.AppleTokenExchangeRequest(
                code, serviceId, clientSecretJwt, callbackUri, codeVerifier));

    String idToken = requireValue(tokenResponse == null ? null : tokenResponse.idToken(), "oauth_apple_exchange_failed");
    AppleIdTokenValidator.AppleIdTokenClaims claims = appleIdTokenValidator.validate(idToken, serviceId);

    String providerUserId = requireValue(claims == null ? null : claims.providerUserId(), "oauth_apple_userinfo_failed");
    String email = requireValue(claims == null ? null : claims.email(), "oauth_apple_userinfo_failed");

    return new OAuthUserInfo(providerUserId, email, claims.emailVerified(), null);
  }

  private String decryptPrivateKey(ProjectSettings settings) {
    String enc = settings.getOauthApplePrivateKeyEnc();
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
      throw new DomainValidationException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }
}
