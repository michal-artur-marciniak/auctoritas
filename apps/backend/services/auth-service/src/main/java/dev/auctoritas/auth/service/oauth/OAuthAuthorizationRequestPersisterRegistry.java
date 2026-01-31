package dev.auctoritas.auth.service.oauth;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OAuthAuthorizationRequestPersisterRegistry {
  private final Map<String, OAuthAuthorizationRequestPersister> byProviderLower;

  public OAuthAuthorizationRequestPersisterRegistry(List<OAuthAuthorizationRequestPersister> persisters) {
    Map<String, OAuthAuthorizationRequestPersister> map = new HashMap<>();
    for (OAuthAuthorizationRequestPersister persister : persisters) {
      if (persister == null || persister.provider() == null || persister.provider().trim().isEmpty()) {
        continue;
      }
      String key = persister.provider().trim().toLowerCase(Locale.ROOT);
      OAuthAuthorizationRequestPersister existing = map.putIfAbsent(key, persister);
      if (existing != null) {
        throw new IllegalStateException(
            "Duplicate OAuthAuthorizationRequestPersister for provider: "
                + persister.provider()
                + " ("
                + existing.getClass().getName()
                + ")");
      }
    }
    this.byProviderLower = Map.copyOf(map);
  }

  public Optional<OAuthAuthorizationRequestPersister> find(String provider) {
    if (provider == null) {
      return Optional.empty();
    }
    String key = provider.trim().toLowerCase(Locale.ROOT);
    if (key.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(byProviderLower.get(key));
  }

  public OAuthAuthorizationRequestPersister require(String provider) {
    return find(provider)
        .orElseThrow(
            () -> new DomainValidationException("oauth_provider_invalid"));
  }
}
