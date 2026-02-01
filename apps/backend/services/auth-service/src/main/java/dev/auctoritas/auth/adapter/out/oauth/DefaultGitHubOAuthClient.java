package dev.auctoritas.auth.adapter.out.oauth;

import dev.auctoritas.auth.domain.exception.DomainExternalServiceException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class DefaultGitHubOAuthClient implements GitHubOAuthClient {
  private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
  private static final String GITHUB_USER_URL = "https://api.github.com/user";
  private static final String GITHUB_USER_EMAILS_URL = "https://api.github.com/user/emails";
  private static final String GITHUB_USER_AGENT = "auctoritas-auth-service";
  private static final String ENC_PREFIX = "ENC:";

  private final RestClient restClient;
  private final TextEncryptor oauthClientSecretEncryptor;

  public DefaultGitHubOAuthClient(
      RestClient.Builder builder,
      @Qualifier("oauthClientSecretEncryptor") TextEncryptor oauthClientSecretEncryptor) {
    this.restClient = builder.build();
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
  }

  @Override
  public GitHubTokenResponse exchangeAuthorizationCode(GitHubTokenExchangeRequest request) {
    if (request == null) {
      throw new DomainValidationException("oauth_github_exchange_failed");
    }

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", value(request.clientId(), "oauth_github_exchange_failed"));
    form.add("client_secret", value(request.clientSecret(), "oauth_github_exchange_failed"));
    form.add("code", value(request.code(), "oauth_github_exchange_failed"));
    form.add("redirect_uri", value(request.redirectUri(), "oauth_github_exchange_failed"));
    form.add(
        "code_verifier",
        decryptCodeVerifier(value(request.codeVerifier(), "oauth_github_exchange_failed")));

    try {
      GitHubTokenResponse response =
          restClient
              .post()
              .uri(GITHUB_TOKEN_URL)
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(GitHubTokenResponse.class);
      if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
        throw new DomainExternalServiceException("oauth_github_exchange_failed");
      }
      return response;
    } catch (RestClientException ex) {
      throw new DomainExternalServiceException("oauth_github_exchange_failed", ex);
    }
  }

  @Override
  public GitHubUser fetchUser(String accessToken) {
    String token = value(accessToken, "oauth_github_userinfo_failed");
    try {
      GitHubUser user =
          restClient
              .get()
              .uri(GITHUB_USER_URL)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
              .header(HttpHeaders.USER_AGENT, GITHUB_USER_AGENT)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(GitHubUser.class);
      if (user == null || user.id() == null) {
        throw new DomainExternalServiceException("oauth_github_userinfo_failed");
      }
      return user;
    } catch (RestClientException ex) {
      throw new DomainExternalServiceException("oauth_github_userinfo_failed", ex);
    }
  }

  @Override
  public List<GitHubUserEmail> fetchUserEmails(String accessToken) {
    String token = value(accessToken, "oauth_github_userinfo_failed");
    try {
      GitHubUserEmail[] emails =
          restClient
              .get()
              .uri(GITHUB_USER_EMAILS_URL)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
              .header(HttpHeaders.USER_AGENT, GITHUB_USER_AGENT)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(GitHubUserEmail[].class);
      if (emails == null) {
        return List.of();
      }
      return Arrays.asList(emails);
    } catch (RestClientException ex) {
      throw new DomainExternalServiceException("oauth_github_userinfo_failed", ex);
    }
  }

  private String decryptCodeVerifier(String codeVerifier) {
    if (codeVerifier == null) {
      throw new DomainValidationException("oauth_github_exchange_failed");
    }

    String trimmed = codeVerifier.trim();
    if (!trimmed.startsWith(ENC_PREFIX)) {
      throw new DomainValidationException("oauth_github_exchange_failed");
    }
    String ciphertext = trimmed.substring(ENC_PREFIX.length());
    try {
      return oauthClientSecretEncryptor.decrypt(ciphertext);
    } catch (Exception ex) {
      throw new DomainValidationException("oauth_github_exchange_failed", ex);
    }
  }

  private static String value(String s, String errorCode) {
    if (s == null) {
      throw new DomainValidationException(errorCode);
    }
    String trimmed = s.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }
}
