package dev.auctoritas.auth.adapters.external.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public interface GitHubOAuthClient {
  GitHubTokenResponse exchangeAuthorizationCode(GitHubTokenExchangeRequest request);

  GitHubUser fetchUser(String accessToken);

  List<GitHubUserEmail> fetchUserEmails(String accessToken);

  record GitHubTokenExchangeRequest(
      String code,
      String clientId,
      String clientSecret,
      String redirectUri,
      String codeVerifier) {}

  record GitHubTokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("token_type") String tokenType,
      String scope) {}

  record GitHubUser(Long id, String login, String name, String email) {}

  record GitHubUserEmail(String email, Boolean primary, Boolean verified, String visibility) {}
}
