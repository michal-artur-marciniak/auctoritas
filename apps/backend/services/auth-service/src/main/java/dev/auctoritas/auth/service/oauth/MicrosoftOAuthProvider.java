package dev.auctoritas.auth.service.oauth;

import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.service.MicrosoftOAuthClient;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MicrosoftOAuthProvider implements OAuthProvider {
  private static final String PROVIDER = "microsoft";
  private static final String DEFAULT_TENANT = "common";
  private static final String AUTHORIZE_URL_TEMPLATE =
      "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize";
  private static final String SCOPE = "openid profile email User.Read";

  private final TextEncryptor oauthClientSecretEncryptor;
  private final MicrosoftOAuthClient microsoftOAuthClient;

  public MicrosoftOAuthProvider(
      TextEncryptor oauthClientSecretEncryptor, MicrosoftOAuthClient microsoftOAuthClient) {
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
    this.microsoftOAuthClient = microsoftOAuthClient;
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
    Object microsoftObj = oauthConfig.get(PROVIDER);
    if (!(microsoftObj instanceof Map<?, ?> microsoftRaw)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_not_configured");
    }

    boolean enabled = Boolean.TRUE.equals(microsoftRaw.get("enabled"));
    String clientId = trimToNull(microsoftRaw.get("clientId"));
    String tenant = normalizeTenant(microsoftRaw.get("tenant"));
    String authorizationEndpoint = AUTHORIZE_URL_TEMPLATE.formatted(tenant);

    if (!enabled || clientId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_not_configured");
    }

    return new OAuthAuthorizeDetails(clientId, authorizationEndpoint, SCOPE);
  }

  @Override
  public String buildAuthorizeUrl(OAuthAuthorizeDetails details, OAuthAuthorizeUrlRequest request) {
    if (details == null || details.clientId() == null || details.clientId().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_not_configured");
    }
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_authorize_failed");
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_not_configured");
    }

    String tenant = normalizeTenantFromSettings(settings);
    String code = requireValue(request == null ? null : request.code(), "oauth_code_missing");
    String callbackUri =
        requireValue(request == null ? null : request.callbackUri(), "oauth_callback_uri_missing");
    String codeVerifier =
        requireValue(request == null ? null : request.codeVerifier(), "oauth_code_verifier_missing");

    MicrosoftOAuthClient.MicrosoftTokenResponse tokenResponse =
        microsoftOAuthClient.exchangeAuthorizationCode(
            new MicrosoftOAuthClient.MicrosoftTokenExchangeRequest(
                code, details.clientId(), clientSecret, callbackUri, codeVerifier, tenant));

    String accessToken =
        requireValue(
            tokenResponse == null ? null : tokenResponse.accessToken(),
            "oauth_microsoft_exchange_failed");
    MicrosoftOAuthClient.MicrosoftUserInfo userInfo = microsoftOAuthClient.fetchUserInfo(accessToken);

    String providerUserId =
        requireValue(userInfo == null ? null : userInfo.sub(), "oauth_microsoft_userinfo_failed");
    String email = trimToNull(userInfo == null ? null : userInfo.email());
    if (email == null) {
      email = trimToNull(userInfo == null ? null : userInfo.preferredUsername());
    }
    String resolvedEmail = requireValue(email, "oauth_microsoft_userinfo_failed");
    String name = trimToNull(userInfo == null ? null : userInfo.name());

    // Microsoft UserInfo does not provide an email_verified claim.
    return new OAuthUserInfo(providerUserId, resolvedEmail, null, name);
  }

  private String decryptClientSecret(ProjectSettings settings) {
    if (settings == null) {
      return null;
    }
    String enc = settings.getOauthMicrosoftClientSecretEnc();
    String decrypted = enc == null || enc.trim().isEmpty() ? null : oauthClientSecretEncryptor.decrypt(enc);
    return decrypted == null ? null : trimToNull(decrypted);
  }

  private static String normalizeTenantFromSettings(ProjectSettings settings) {
    if (settings == null) {
      return DEFAULT_TENANT;
    }
    Map<String, Object> oauthConfig = settings.getOauthConfig() == null ? Map.of() : settings.getOauthConfig();
    Object microsoftObj = oauthConfig.get(PROVIDER);
    if (microsoftObj instanceof Map<?, ?> microsoftRaw) {
      return normalizeTenant(microsoftRaw.get("tenant"));
    }
    return DEFAULT_TENANT;
  }

  private static String normalizeTenant(Object raw) {
    String tenant = trimToNull(raw);
    if (tenant == null) {
      return DEFAULT_TENANT;
    }
    if (tenant.contains("/") || tenant.contains("?") || tenant.contains("#")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_not_configured");
    }
    return tenant;
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
