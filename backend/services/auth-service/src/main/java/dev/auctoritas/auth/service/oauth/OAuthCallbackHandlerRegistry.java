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
public class OAuthCallbackHandlerRegistry {
  private final Map<String, OAuthCallbackHandler> byProviderLower;

  public OAuthCallbackHandlerRegistry(List<OAuthCallbackHandler> handlers) {
    Map<String, OAuthCallbackHandler> map = new HashMap<>();
    for (OAuthCallbackHandler handler : handlers) {
      if (handler == null || handler.provider() == null || handler.provider().trim().isEmpty()) {
        continue;
      }
      String key = handler.provider().trim().toLowerCase(Locale.ROOT);
      OAuthCallbackHandler existing = map.putIfAbsent(key, handler);
      if (existing != null) {
        throw new IllegalStateException(
            "Duplicate OAuthCallbackHandler for provider: "
                + handler.provider()
                + " ("
                + existing.getClass().getName()
                + ")");
      }
    }
    this.byProviderLower = Map.copyOf(map);
  }

  public Optional<OAuthCallbackHandler> find(String provider) {
    if (provider == null) {
      return Optional.empty();
    }
    String key = provider.trim().toLowerCase(Locale.ROOT);
    if (key.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(byProviderLower.get(key));
  }

  public OAuthCallbackHandler require(String provider) {
    return find(provider)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_provider_invalid"));
  }
}
