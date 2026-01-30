package dev.auctoritas.auth.adapters.external.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface AppleOAuthClient {
  AppleTokenResponse exchangeAuthorizationCode(AppleTokenExchangeRequest request);

  record AppleTokenExchangeRequest(
      String code,
      String clientId,
      String clientSecret,
      String redirectUri,
      String codeVerifier) {}

  record AppleTokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("id_token") String idToken,
      @JsonProperty("refresh_token") String refreshToken,
      @JsonProperty("token_type") String tokenType,
      @JsonProperty("expires_in") Long expiresIn) {}
}
