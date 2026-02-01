package dev.auctoritas.auth.application.port.in.system;

import java.security.PublicKey;

/**
 * Use case for retrieving JWKS (JSON Web Key Set) information.
 */
public interface JwksUseCase {
  PublicKey getPublicKey();
}
