package dev.auctoritas.auth.infrastructure.oauth;

import dev.auctoritas.auth.domain.exception.DomainExternalServiceException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class DefaultGoogleOAuthClient implements GoogleOAuthClient {
  private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
  private static final String GOOGLE_USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";
  private static final String ENC_PREFIX = "ENC:";

  private final RestClient restClient;
  private final TextEncryptor oauthClientSecretEncryptor;

  public DefaultGoogleOAuthClient(
      RestClient.Builder builder,
      @Qualifier("oauthClientSecretEncryptor") TextEncryptor oauthClientSecretEncryptor) {
    this.restClient = builder.build();
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
  }

  @Override
  public GoogleTokenResponse exchangeAuthorizationCode(GoogleTokenExchangeRequest request) {
    if (request == null) {
      throw new DomainValidationException("oauth_google_exchange_failed");
    }

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", value(request.code(), "oauth_google_exchange_failed"));
    form.add("client_id", value(request.clientId(), "oauth_google_exchange_failed"));
    form.add("client_secret", value(request.clientSecret(), "oauth_google_exchange_failed"));
    form.add("redirect_uri", value(request.redirectUri(), "oauth_google_exchange_failed"));
    form.add(
        "code_verifier",
        decryptCodeVerifier(value(request.codeVerifier(), "oauth_google_exchange_failed")));

    try {
      GoogleTokenResponse response =
          restClient
              .post()
              .uri(GOOGLE_TOKEN_URL)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(GoogleTokenResponse.class);
      if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
        throw new DomainExternalServiceException("oauth_google_exchange_failed");
      }
      return response;
    } catch (RestClientException ex) {
      throw new DomainExternalServiceException("oauth_google_exchange_failed", ex);
    }
  }

  @Override
  public GoogleUserInfo fetchUserInfo(String accessToken) {
    String token = value(accessToken, "oauth_google_userinfo_failed");
    try {
      GoogleUserInfo info =
          restClient
              .get()
              .uri(GOOGLE_USERINFO_URL)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(GoogleUserInfo.class);
      if (info == null || info.sub() == null || info.sub().isBlank()) {
        throw new DomainExternalServiceException("oauth_google_userinfo_failed");
      }
      if (info.email() == null || info.email().isBlank()) {
        throw new DomainExternalServiceException("oauth_google_userinfo_failed");
      }
      return info;
    } catch (RestClientException ex) {
      throw new DomainExternalServiceException("oauth_google_userinfo_failed", ex);
    }
  }

  private String decryptCodeVerifier(String codeVerifier) {
    if (codeVerifier == null) {
      throw new DomainValidationException("oauth_google_exchange_failed");
    }

    String trimmed = codeVerifier.trim();
    if (!trimmed.startsWith(ENC_PREFIX)) {
      throw new DomainValidationException("oauth_google_exchange_failed");
    }
    String ciphertext = trimmed.substring(ENC_PREFIX.length());
    try {
      return oauthClientSecretEncryptor.decrypt(ciphertext);
    } catch (Exception ex) {
      throw new DomainValidationException("oauth_google_exchange_failed", ex);
    }
  }

  private static String value(String s, String errorCode) {
    if (s == null) {
      throw new DomainValidationException(errorCode);
    }
    String trimmed = s.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }
}
