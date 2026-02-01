package dev.auctoritas.auth.infrastructure.oauth;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.application.port.out.oauth.OAuthProviderPort;
import dev.auctoritas.auth.application.oauth.OAuthAuthorizeDetails;
import dev.auctoritas.auth.application.oauth.OAuthAuthorizeUrlRequest;
import dev.auctoritas.auth.application.oauth.OAuthTokenExchangeRequest;
import dev.auctoritas.auth.application.oauth.OAuthUserInfo;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GitHubOAuthProvider implements OAuthProviderPort {
  private static final String PROVIDER = "github";
  private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
  private static final String SCOPE = "read:user user:email";

  private final TextEncryptor oauthClientSecretEncryptor;
  private final GitHubOAuthClient gitHubOAuthClient;

  public GitHubOAuthProvider(
      TextEncryptor oauthClientSecretEncryptor, GitHubOAuthClient gitHubOAuthClient) {
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
    this.gitHubOAuthClient = gitHubOAuthClient;
  }

  @Override
  public String name() {
    return PROVIDER;
  }

  @Override
  public OAuthAuthorizeDetails getAuthorizeDetails(ProjectSettings settings) {
    if (settings == null) {
      throw new DomainValidationException("project_settings_missing");
    }
    Map<String, Object> oauthConfig = settings.getOauthConfig() == null ? Map.of() : settings.getOauthConfig();
    Object githubObj = oauthConfig.get(PROVIDER);
    if (!(githubObj instanceof Map<?, ?> githubRaw)) {
      throw new DomainValidationException("oauth_github_not_configured");
    }

    boolean enabled = Boolean.TRUE.equals(githubRaw.get("enabled"));
    String clientId = trimToNull(githubRaw.get("clientId"));

    if (!enabled || clientId == null) {
      throw new DomainValidationException("oauth_github_not_configured");
    }

    return new OAuthAuthorizeDetails(clientId, AUTHORIZE_URL, SCOPE);
  }

  @Override
  public String buildAuthorizeUrl(OAuthAuthorizeDetails details, OAuthAuthorizeUrlRequest request) {
    if (details == null || details.clientId() == null || details.clientId().isBlank()) {
      throw new DomainValidationException("oauth_github_not_configured");
    }
    if (request == null) {
      throw new DomainValidationException("oauth_github_authorize_failed");
    }

    return UriComponentsBuilder.fromUriString(details.authorizationEndpoint())
        .queryParam("client_id", details.clientId())
        .queryParam("redirect_uri", request.callbackUri())
        .queryParam("scope", details.scope())
        .queryParam("state", request.state())
        .queryParam("code_challenge", request.codeChallenge())
        .queryParam("code_challenge_method", request.codeChallengeMethod())
        .build()
        .encode()
        .toUriString();
  }

  @Override
  public OAuthUserInfo exchangeAuthorizationCode(ProjectSettings settings, OAuthTokenExchangeRequest request) {
    OAuthAuthorizeDetails details = getAuthorizeDetails(settings);

    String clientSecret = decryptClientSecret(settings);
    if (clientSecret == null) {
      throw new DomainValidationException("oauth_github_not_configured");
    }

    String code = requireValue(request == null ? null : request.code(), "oauth_code_missing");
    String callbackUri = requireValue(request == null ? null : request.callbackUri(), "oauth_callback_uri_missing");
    String codeVerifier = requireValue(request == null ? null : request.codeVerifier(), "oauth_code_verifier_missing");

    GitHubOAuthClient.GitHubTokenResponse tokenResponse =
        gitHubOAuthClient.exchangeAuthorizationCode(
            new GitHubOAuthClient.GitHubTokenExchangeRequest(
                code, details.clientId(), clientSecret, callbackUri, codeVerifier));

    String accessToken = requireValue(tokenResponse == null ? null : tokenResponse.accessToken(), "oauth_github_exchange_failed");
    GitHubOAuthClient.GitHubUser user = gitHubOAuthClient.fetchUser(accessToken);
    List<GitHubOAuthClient.GitHubUserEmail> emails = gitHubOAuthClient.fetchUserEmails(accessToken);

    String providerUserId = requireValue(user == null || user.id() == null ? null : user.id().toString(), "oauth_github_userinfo_failed");

    SelectedEmail selected = selectEmail(user, emails);
    String email = requireValue(selected.email, "oauth_github_userinfo_failed");
    Boolean emailVerified = selected.verified;
    String name = trimToNull(user == null ? null : user.name());
    if (name == null) {
      name = trimToNull(user == null ? null : user.login());
    }

    return new OAuthUserInfo(providerUserId, email, emailVerified, name);
  }

  private SelectedEmail selectEmail(
      GitHubOAuthClient.GitHubUser user, List<GitHubOAuthClient.GitHubUserEmail> emails) {
    if (emails != null) {
      for (GitHubOAuthClient.GitHubUserEmail e : emails) {
        if (e != null && Boolean.TRUE.equals(e.primary()) && trimToNull(e.email()) != null) {
          return new SelectedEmail(trimToNull(e.email()), e.verified());
        }
      }
      for (GitHubOAuthClient.GitHubUserEmail e : emails) {
        if (e != null && Boolean.TRUE.equals(e.verified()) && trimToNull(e.email()) != null) {
          return new SelectedEmail(trimToNull(e.email()), e.verified());
        }
      }
      for (GitHubOAuthClient.GitHubUserEmail e : emails) {
        if (e != null && trimToNull(e.email()) != null) {
          return new SelectedEmail(trimToNull(e.email()), e.verified());
        }
      }
    }

    String fallback = trimToNull(user == null ? null : user.email());
    if (fallback != null) {
      return new SelectedEmail(fallback, null);
    }
    return new SelectedEmail(null, null);
  }

  private String decryptClientSecret(ProjectSettings settings) {
    String enc = settings.getOauthGithubClientSecretEnc();
    String decrypted = enc == null || enc.trim().isEmpty() ? null : oauthClientSecretEncryptor.decrypt(enc);
    return decrypted == null ? null : trimToNull(decrypted);
  }

  private static String trimToNull(Object value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.toString().trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new DomainValidationException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }

  private record SelectedEmail(String email, Boolean verified) {}
}
