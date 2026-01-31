package dev.auctoritas.auth.adapters.external.oauth;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.ports.oauth.OAuthProviderPort;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeDetails;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeUrlRequest;
import dev.auctoritas.auth.service.oauth.OAuthTokenExchangeRequest;
import dev.auctoritas.auth.service.oauth.OAuthUserInfo;
import java.util.Map;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class FacebookOAuthProvider implements OAuthProviderPort {
  private static final String PROVIDER = "facebook";
  private static final String AUTHORIZE_URL = "https://www.facebook.com/v18.0/dialog/oauth";
  private static final String SCOPE = "email public_profile";

  private final TextEncryptor oauthClientSecretEncryptor;
  private final FacebookOAuthClient facebookOAuthClient;

  public FacebookOAuthProvider(
      TextEncryptor oauthClientSecretEncryptor, FacebookOAuthClient facebookOAuthClient) {
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
    this.facebookOAuthClient = facebookOAuthClient;
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
    Object facebookObj = oauthConfig.get(PROVIDER);
    if (!(facebookObj instanceof Map<?, ?> facebookRaw)) {
      throw new DomainValidationException("oauth_facebook_not_configured");
    }

    boolean enabled = Boolean.TRUE.equals(facebookRaw.get("enabled"));
    String clientId = trimToNull(facebookRaw.get("clientId"));

    if (!enabled || clientId == null) {
      throw new DomainValidationException("oauth_facebook_not_configured");
    }

    return new OAuthAuthorizeDetails(clientId, AUTHORIZE_URL, SCOPE);
  }

  @Override
  public String buildAuthorizeUrl(OAuthAuthorizeDetails details, OAuthAuthorizeUrlRequest request) {
    if (details == null || details.clientId() == null || details.clientId().isBlank()) {
      throw new DomainValidationException("oauth_facebook_not_configured");
    }
    if (request == null) {
      throw new DomainValidationException("oauth_facebook_authorize_failed");
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
      throw new DomainValidationException("oauth_facebook_not_configured");
    }

    String code = requireValue(request == null ? null : request.code(), "oauth_code_missing");
    String callbackUri =
        requireValue(request == null ? null : request.callbackUri(), "oauth_callback_uri_missing");
    String codeVerifier =
        requireValue(request == null ? null : request.codeVerifier(), "oauth_code_verifier_missing");

    FacebookOAuthClient.FacebookTokenResponse tokenResponse =
        facebookOAuthClient.exchangeAuthorizationCode(
            new FacebookOAuthClient.FacebookTokenExchangeRequest(
                code, details.clientId(), clientSecret, callbackUri, codeVerifier));

    String accessToken =
        requireValue(
            tokenResponse == null ? null : tokenResponse.accessToken(),
            "oauth_facebook_exchange_failed");
    FacebookOAuthClient.FacebookUserInfo userInfo = facebookOAuthClient.fetchUserInfo(accessToken);

    String providerUserId =
        requireValue(userInfo == null ? null : userInfo.id(), "oauth_facebook_userinfo_failed");
    String email = requireValue(userInfo == null ? null : userInfo.email(), "oauth_facebook_userinfo_failed");
    String name = trimToNull(userInfo == null ? null : userInfo.name());

    // Facebook userinfo does not provide an email_verified claim.
    return new OAuthUserInfo(providerUserId, email, null, name);
  }

  private String decryptClientSecret(ProjectSettings settings) {
    String enc = settings.getOauthFacebookClientSecretEnc();
    String decrypted =
        enc == null || enc.trim().isEmpty() ? null : oauthClientSecretEncryptor.decrypt(enc);
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
