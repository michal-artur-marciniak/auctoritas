package dev.auctoritas.auth.service.oauth;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OAuthProviderRegistry {
  private final Map<String, OAuthProvider> byNameLower;

  public OAuthProviderRegistry(List<OAuthProvider> providers) {
    Map<String, OAuthProvider> map = new HashMap<>();
    for (OAuthProvider provider : providers) {
      if (provider == null || provider.name() == null || provider.name().trim().isEmpty()) {
        continue;
      }
      String key = provider.name().trim().toLowerCase(Locale.ROOT);
      OAuthProvider existing = map.putIfAbsent(key, provider);
      if (existing != null) {
        throw new IllegalStateException(
            "Duplicate OAuthProvider for name: " + provider.name() + " (" + existing.getClass().getName() + ")");
      }
    }
    this.byNameLower = Map.copyOf(map);
  }

  public Optional<OAuthProvider> find(String providerName) {
    if (providerName == null) {
      return Optional.empty();
    }
    String key = providerName.trim().toLowerCase(Locale.ROOT);
    if (key.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(byNameLower.get(key));
  }

  public OAuthProvider require(String providerName) {
    return find(providerName)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_provider_invalid"));
  }
}
