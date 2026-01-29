package dev.auctoritas.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class OAuthAuthorizationRequestPersisterRegistryTest {

  @Test
  void requireUnknownProviderThrowsBadRequestWithOauthProviderInvalid() {
    OAuthAuthorizationRequestPersisterRegistry registry =
        new OAuthAuthorizationRequestPersisterRegistry(List.of(testPersister("google")));

    ResponseStatusException ex =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () -> registry.require("unknown"), ResponseStatusException.class);

    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getReason()).isEqualTo("oauth_provider_invalid");
  }

  @Test
  void requireNormalizesProviderByLowercasingAndTrimming() {
    OAuthAuthorizationRequestPersister google = testPersister("google");
    OAuthAuthorizationRequestPersisterRegistry registry =
        new OAuthAuthorizationRequestPersisterRegistry(List.of(google));

    assertThat(registry.require("  GOOGLE  ")).isSameAs(google);
  }

  @Test
  void rejectsDuplicateProviderKeys() {
    assertThatThrownBy(
            () ->
                new OAuthAuthorizationRequestPersisterRegistry(
                    List.of(testPersister("google"), testPersister("google"))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Duplicate OAuthAuthorizationRequestPersister");
  }

  private static OAuthAuthorizationRequestPersister testPersister(String provider) {
    return new OAuthAuthorizationRequestPersister() {
      @Override
      public String provider() {
        return provider;
      }

      @Override
      public void createAuthorizationRequest(OAuthAuthorizationRequestCreateRequest request) {
        // no-op
      }
    };
  }
}
