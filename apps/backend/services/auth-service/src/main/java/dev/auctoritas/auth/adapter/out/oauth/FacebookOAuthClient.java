package dev.auctoritas.auth.adapter.out.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface FacebookOAuthClient {
  FacebookTokenResponse exchangeAuthorizationCode(FacebookTokenExchangeRequest request);

  FacebookUserInfo fetchUserInfo(String accessToken);

  record FacebookTokenExchangeRequest(
      String code,
      String clientId,
      String clientSecret,
      String redirectUri,
      String codeVerifier) {}

  record FacebookTokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("token_type") String tokenType,
      @JsonProperty("expires_in") Long expiresIn,
      @JsonProperty("id_token") String idToken) {}

  record FacebookUserInfo(String id, String name, String email) {}
}
