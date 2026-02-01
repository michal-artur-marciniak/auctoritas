package dev.auctoritas.auth.application.oauth;

/** Persists an OAuth authorization request (state/PKCE verifier) for a given provider. */
public interface OAuthAuthorizationRequestPersister {
  /** Provider key used in requests (e.g. "google"). */
  String provider();

  void createAuthorizationRequest(OAuthAuthorizationRequestCreateRequest request);
}
