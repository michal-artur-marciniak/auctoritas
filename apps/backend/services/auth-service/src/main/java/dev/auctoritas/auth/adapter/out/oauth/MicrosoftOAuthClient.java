package dev.auctoritas.auth.adapter.out.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface MicrosoftOAuthClient {
  MicrosoftTokenResponse exchangeAuthorizationCode(MicrosoftTokenExchangeRequest request);

  MicrosoftUserInfo fetchUserInfo(String accessToken);

  record MicrosoftTokenExchangeRequest(
      String code,
      String clientId,
      String clientSecret,
      String redirectUri,
      String codeVerifier,
      String tenant) {}

  record MicrosoftTokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("refresh_token") String refreshToken,
      @JsonProperty("id_token") String idToken,
      @JsonProperty("token_type") String tokenType,
      @JsonProperty("expires_in") Long expiresIn,
      @JsonProperty("scope") String scope) {}

  record MicrosoftUserInfo(
      String sub,
      String name,
      String email,
      @JsonProperty("preferred_username") String preferredUsername) {}
}
