package dev.auctoritas.auth.adapter.out.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface GoogleOAuthClient {
  GoogleTokenResponse exchangeAuthorizationCode(GoogleTokenExchangeRequest request);

  GoogleUserInfo fetchUserInfo(String accessToken);

  record GoogleTokenExchangeRequest(
      String code,
      String clientId,
      String clientSecret,
      String redirectUri,
      String codeVerifier) {}

  record GoogleTokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("refresh_token") String refreshToken,
      @JsonProperty("id_token") String idToken,
      @JsonProperty("token_type") String tokenType,
      @JsonProperty("expires_in") Long expiresIn,
      @JsonProperty("scope") String scope) {}

  record GoogleUserInfo(
      String sub,
      String email,
      @JsonProperty("email_verified") Boolean emailVerified,
      String name) {}
}
