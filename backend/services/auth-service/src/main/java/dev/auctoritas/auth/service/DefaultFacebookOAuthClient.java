package dev.auctoritas.auth.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefaultFacebookOAuthClient implements FacebookOAuthClient {
  private static final String FACEBOOK_TOKEN_URL = "https://graph.facebook.com/v18.0/oauth/access_token";
  private static final String FACEBOOK_ME_URL = "https://graph.facebook.com/me";
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

    String url =
        FACEBOOK_TOKEN_URL
            + "?client_id="
            + urlEncode(value(request.clientId(), "oauth_facebook_exchange_failed"))
            + "&client_secret="
            + urlEncode(value(request.clientSecret(), "oauth_facebook_exchange_failed"))
            + "&redirect_uri="
            + urlEncode(value(request.redirectUri(), "oauth_facebook_exchange_failed"))
            + "&code="
            + urlEncode(value(request.code(), "oauth_facebook_exchange_failed"))
            + "&code_verifier="
            + urlEncode(
                decryptCodeVerifier(value(request.codeVerifier(), "oauth_facebook_exchange_failed")));

    try {
      FacebookTokenResponse response =
          restClient
              .get()
              .uri(url)
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

    String url =
        FACEBOOK_ME_URL + "?fields=" + urlEncode("id,name,email") + "&access_token=" + urlEncode(token);

    try {
      FacebookUserInfo info =
          restClient
              .get()
              .uri(url)
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

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
