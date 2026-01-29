package dev.auctoritas.auth.service;

import org.springframework.beans.factory.annotation.Qualifier;
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
public class DefaultAppleOAuthClient implements AppleOAuthClient {
  private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
  private static final String ENC_PREFIX = "ENC:";

  private final RestClient restClient;
  private final TextEncryptor oauthClientSecretEncryptor;

  public DefaultAppleOAuthClient(
      RestClient.Builder builder,
      @Qualifier("oauthClientSecretEncryptor") TextEncryptor oauthClientSecretEncryptor) {
    this.restClient = builder.build();
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
  }

  @Override
  public AppleTokenResponse exchangeAuthorizationCode(AppleTokenExchangeRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_apple_exchange_failed");
    }

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", value(request.code(), "oauth_apple_exchange_failed"));
    form.add("client_id", value(request.clientId(), "oauth_apple_exchange_failed"));
    form.add("client_secret", value(request.clientSecret(), "oauth_apple_exchange_failed"));
    form.add("redirect_uri", value(request.redirectUri(), "oauth_apple_exchange_failed"));
    form.add(
        "code_verifier",
        decryptCodeVerifier(value(request.codeVerifier(), "oauth_apple_exchange_failed")));

    try {
      AppleTokenResponse response =
          restClient
              .post()
              .uri(APPLE_TOKEN_URL)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(AppleTokenResponse.class);
      if (response == null || response.idToken() == null || response.idToken().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_apple_exchange_failed");
      }
      return response;
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "oauth_apple_exchange_failed", ex);
    }
  }

  private String decryptCodeVerifier(String codeVerifier) {
    if (codeVerifier == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_apple_exchange_failed");
    }

    String trimmed = codeVerifier.trim();
    if (!trimmed.startsWith(ENC_PREFIX)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_apple_exchange_failed");
    }
    String ciphertext = trimmed.substring(ENC_PREFIX.length());
    try {
      return oauthClientSecretEncryptor.decrypt(ciphertext);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_apple_exchange_failed", ex);
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
