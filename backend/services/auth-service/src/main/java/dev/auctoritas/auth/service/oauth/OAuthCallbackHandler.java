package dev.auctoritas.auth.service.oauth;

/** Handles a provider callback and returns the app redirect URL. */
public interface OAuthCallbackHandler {
  /** Provider key used in requests (e.g. "google"). */
  String provider();

  String handleCallback(OAuthCallbackHandleRequest request);
}
