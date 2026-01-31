package dev.auctoritas.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class OAuthCallbackHandlerRegistryTest {

  @Test
  void requireUnknownProviderThrowsBadRequestWithOauthProviderInvalid() {
    OAuthCallbackHandlerRegistry registry =
        new OAuthCallbackHandlerRegistry(List.of(testHandler("google")));

    assertThatThrownBy(() -> registry.require("unknown"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("oauth_provider_invalid");
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
