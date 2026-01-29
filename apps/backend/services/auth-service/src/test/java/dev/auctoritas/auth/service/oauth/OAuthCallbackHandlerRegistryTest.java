package dev.auctoritas.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class OAuthCallbackHandlerRegistryTest {

  @Test
  void requireUnknownProviderThrowsBadRequestWithOauthProviderInvalid() {
    OAuthCallbackHandlerRegistry registry =
        new OAuthCallbackHandlerRegistry(List.of(testHandler("google")));

    ResponseStatusException ex =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () -> registry.require("unknown"), ResponseStatusException.class);

    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getReason()).isEqualTo("oauth_provider_invalid");
  }

  @Test
  void requireNormalizesProviderByLowercasingAndTrimming() {
    OAuthCallbackHandler google = testHandler("google");
    OAuthCallbackHandlerRegistry registry = new OAuthCallbackHandlerRegistry(List.of(google));

    assertThat(registry.require("  GOOGLE  ")).isSameAs(google);
  }

  @Test
  void rejectsDuplicateProviderKeys() {
    assertThatThrownBy(
            () ->
                new OAuthCallbackHandlerRegistry(
                    List.of(testHandler("google"), testHandler("google"))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Duplicate OAuthCallbackHandler");
  }

  private static OAuthCallbackHandler testHandler(String provider) {
    return new OAuthCallbackHandler() {
      @Override
      public String provider() {
        return provider;
      }

      @Override
      public String handleCallback(OAuthCallbackHandleRequest request) {
        return "ok";
      }
    };
  }
}
