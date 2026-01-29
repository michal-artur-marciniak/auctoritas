package dev.auctoritas.auth.service;

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
public class DefaultFacebookOAuthClient implements FacebookOAuthClient {
  private static final String FACEBOOK_TOKEN_URL = "https://graph.facebook.com/v19.0/oauth/access_token";
  private static final String FACEBOOK_ME_URL = "https://graph.facebook.com/v19.0/me";
  private static final String ENC_PREFIX = "ENC:";

  private final RestClient restClient;
  private final TextEncryptor oauthClientSecretEncryptor;

  public DefaultFacebookOAuthClient(
      RestClient.Builder builder,
      @Qualifier("oauthClientSecretEncryptor") TextEncryptor oauthClientSecretEncryptor) {
    this.restClient = builder.build();
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
  }

  @Override
  public FacebookTokenResponse exchangeAuthorizationCode(FacebookTokenExchangeRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_facebook_exchange_failed");
    }

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", value(request.clientId(), "oauth_facebook_exchange_failed"));
    form.add("client_secret", value(request.clientSecret(), "oauth_facebook_exchange_failed"));
    form.add("redirect_uri", value(request.redirectUri(), "oauth_facebook_exchange_failed"));
    form.add("code", value(request.code(), "oauth_facebook_exchange_failed"));
    form.add(
        "code_verifier",
        decryptCodeVerifier(value(request.codeVerifier(), "oauth_facebook_exchange_failed")));

    try {
      FacebookTokenResponse response =
          restClient
              .post()
              .uri(FACEBOOK_TOKEN_URL)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(FacebookTokenResponse.class);
      if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_facebook_exchange_failed");
      }
      return response;
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_facebook_exchange_failed", ex);
    }
  }

  @Override
  public FacebookUserInfo fetchUserInfo(String accessToken) {
    String token = value(accessToken, "oauth_facebook_userinfo_failed");

    try {
      FacebookUserInfo info =
          restClient
              .get()
              .uri(FACEBOOK_ME_URL + "?fields=id,name,email")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(FacebookUserInfo.class);
      if (info == null || info.id() == null || info.id().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_facebook_userinfo_failed");
      }
      return info;
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_facebook_userinfo_failed", ex);
    }
  }

  private String decryptCodeVerifier(String codeVerifier) {
    if (codeVerifier == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_facebook_exchange_failed");
    }

    String trimmed = codeVerifier.trim();
    if (!trimmed.startsWith(ENC_PREFIX)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_facebook_exchange_failed");
    }
    String ciphertext = trimmed.substring(ENC_PREFIX.length());
    try {
      return oauthClientSecretEncryptor.decrypt(ciphertext);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_facebook_exchange_failed", ex);
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
