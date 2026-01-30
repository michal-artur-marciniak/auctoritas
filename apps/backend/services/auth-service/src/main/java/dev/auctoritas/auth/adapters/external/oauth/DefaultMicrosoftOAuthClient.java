package dev.auctoritas.auth.adapters.external.oauth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefaultMicrosoftOAuthClient implements MicrosoftOAuthClient {
  private static final String MICROSOFT_TOKEN_URL_TEMPLATE =
      "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
  private static final String MICROSOFT_USERINFO_URL = "https://graph.microsoft.com/oidc/userinfo";
  private static final String ENC_PREFIX = "ENC:";

  private final RestClient restClient;
  private final TextEncryptor oauthClientSecretEncryptor;

  public DefaultMicrosoftOAuthClient(
      RestClient.Builder builder,
      @Qualifier("oauthClientSecretEncryptor") TextEncryptor oauthClientSecretEncryptor) {
    this.restClient = builder.build();
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
  }

  @Override
  public MicrosoftTokenResponse exchangeAuthorizationCode(MicrosoftTokenExchangeRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_exchange_failed");
    }

    String tenant = value(request.tenant(), "oauth_microsoft_exchange_failed");
    String tokenUrl = MICROSOFT_TOKEN_URL_TEMPLATE.formatted(tenant);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", value(request.code(), "oauth_microsoft_exchange_failed"));
    form.add("client_id", value(request.clientId(), "oauth_microsoft_exchange_failed"));
    form.add("client_secret", value(request.clientSecret(), "oauth_microsoft_exchange_failed"));
    form.add("redirect_uri", value(request.redirectUri(), "oauth_microsoft_exchange_failed"));
    form.add(
        "code_verifier",
        decryptCodeVerifier(value(request.codeVerifier(), "oauth_microsoft_exchange_failed")));

    try {
      MicrosoftTokenResponse response =
          restClient
              .post()
              .uri(tokenUrl)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(MicrosoftTokenResponse.class);
      if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_microsoft_exchange_failed");
      }
      return response;
    } catch (RestClientException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "oauth_microsoft_exchange_failed", ex);
    }
  }

  @Override
  public MicrosoftUserInfo fetchUserInfo(String accessToken) {
    String token = value(accessToken, "oauth_microsoft_userinfo_failed");
    try {
      MicrosoftUserInfo info =
          restClient
              .get()
              .uri(MICROSOFT_USERINFO_URL)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(MicrosoftUserInfo.class);
      if (info == null || info.sub() == null || info.sub().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_microsoft_userinfo_failed");
      }
      return info;
    } catch (RestClientException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "oauth_microsoft_userinfo_failed", ex);
    }
  }

  private String decryptCodeVerifier(String codeVerifier) {
    if (codeVerifier == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_exchange_failed");
    }

    String trimmed = codeVerifier.trim();
    if (!trimmed.startsWith(ENC_PREFIX)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_exchange_failed");
    }
    String ciphertext = trimmed.substring(ENC_PREFIX.length());
    try {
      return oauthClientSecretEncryptor.decrypt(ciphertext);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_exchange_failed", ex);
    }
  }

  private static String value(String s, String errorCode) {
    if (s == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    String trimmed = s.trim();
    if (trimmed.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    return trimmed;
  }
}
