package dev.auctoritas.auth.application.oauth;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.application.port.out.oauth.OAuthProviderPort;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Registry for OAuth providers keyed by provider name.
 */
@Component
public class OAuthProviderRegistry {
  private final Map<String, OAuthProviderPort> byNameLower;

  public OAuthProviderRegistry(List<OAuthProviderPort> providers) {
    Map<String, OAuthProviderPort> map = new HashMap<>();
    for (OAuthProviderPort provider : providers) {
      if (provider == null || provider.name() == null || provider.name().trim().isEmpty()) {
        continue;
      }
      String key = provider.name().trim().toLowerCase(Locale.ROOT);
      OAuthProviderPort existing = map.putIfAbsent(key, provider);
      if (existing != null) {
        throw new IllegalStateException(
            "Duplicate OAuthProvider for name: " + provider.name() + " (" + existing.getClass().getName() + ")");
      }
    }
    this.byNameLower = Map.copyOf(map);
  }

  public Optional<OAuthProviderPort> find(String providerName) {
    if (providerName == null) {
      return Optional.empty();
    }
    String key = providerName.trim().toLowerCase(Locale.ROOT);
    if (key.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(byNameLower.get(key));
  }

  public OAuthProviderPort require(String providerName) {
    return find(providerName)
        .orElseThrow(
            () -> new DomainValidationException("oauth_provider_invalid"));
  }
}
