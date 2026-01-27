package dev.auctoritas.auth.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefaultGoogleOAuthClient implements GoogleOAuthClient {
  private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
  private static final String GOOGLE_USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_exchange_failed");
    }

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", value(request.code()));
    form.add("client_id", value(request.clientId()));
    form.add("client_secret", value(request.clientSecret()));
    form.add("redirect_uri", value(request.redirectUri()));
    form.add("code_verifier", decryptCodeVerifier(value(request.codeVerifier())));

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
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_google_exchange_failed");
      }
      return response;
    } catch (RestClientException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "oauth_google_exchange_failed", ex);
    }
  }

  @Override
  public GoogleUserInfo fetchUserInfo(String accessToken) {
    String token = value(accessToken);
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
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_google_userinfo_failed");
      }
      if (info.email() == null || info.email().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_google_userinfo_failed");
      }
      return info;
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_google_userinfo_failed", ex);
    }
  }

  private String decryptCodeVerifier(String codeVerifier) {
    try {
      return oauthClientSecretEncryptor.decrypt(codeVerifier);
    } catch (Exception ex) {
      // Backward compatibility if an older row stored plaintext.
      return codeVerifier;
    }
  }

  private static String value(String s) {
    if (s == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_exchange_failed");
    }
    String trimmed = s.trim();
    if (trimmed.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_exchange_failed");
    }
    return trimmed;
  }
}
