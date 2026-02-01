package dev.auctoritas.auth.domain.project;

/**
 * Status values for API keys in the auth domain.
 */
public enum ApiKeyStatus {
  ACTIVE,
  REVOKED,
  EXPIRED,
  LIMIT_REACHED
}
