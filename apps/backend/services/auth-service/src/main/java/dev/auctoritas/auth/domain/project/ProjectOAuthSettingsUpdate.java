package dev.auctoritas.auth.domain.project;

import java.util.Map;

/**
 * Result of validating and normalizing an OAuth settings patch.
 */
public record ProjectOAuthSettingsUpdate(
    Map<String, Object> oauthConfig,
    SecretUpdate googleClientSecret,
    SecretUpdate githubClientSecret,
    SecretUpdate microsoftClientSecret,
    SecretUpdate facebookClientSecret,
    SecretUpdate applePrivateKey) {

  /**
   * Captures whether a secret update was provided and its cleared or new value.
   */
  public record SecretUpdate(boolean provided, String value) {
    public static SecretUpdate none() {
      return new SecretUpdate(false, null);
    }

    public static SecretUpdate clear() {
      return new SecretUpdate(true, null);
    }

    public static SecretUpdate set(String value) {
      return new SecretUpdate(true, value);
    }
  }
}
