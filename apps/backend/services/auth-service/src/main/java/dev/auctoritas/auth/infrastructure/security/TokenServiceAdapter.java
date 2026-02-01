package dev.auctoritas.auth.infrastructure.security;

import dev.auctoritas.auth.application.port.out.security.TokenHasherPort;
import dev.auctoritas.auth.application.TokenService;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link TokenService} via {@link TokenHasherPort}.
 */
@Component
public class TokenServiceAdapter implements TokenHasherPort {
  private final TokenService tokenService;

  public TokenServiceAdapter(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  public String generateRefreshToken() {
    return tokenService.generateRefreshToken();
  }

  @Override
  public String generatePasswordResetToken() {
    return tokenService.generatePasswordResetToken();
  }

  @Override
  public String generateEmailVerificationToken() {
    return tokenService.generateEmailVerificationToken();
  }

  @Override
  public String generateOAuthExchangeCode() {
    return tokenService.generateOAuthExchangeCode();
  }

  @Override
  public String generateOAuthState() {
    return tokenService.generateOAuthState();
  }

  @Override
  public String generateOAuthCodeVerifier() {
    return tokenService.generateOAuthCodeVerifier();
  }

  @Override
  public String generateEmailVerificationCode() {
    return tokenService.generateEmailVerificationCode();
  }

  @Override
  public String hashToken(String token) {
    return tokenService.hashToken(token);
  }

  @Override
  public Instant getRefreshTokenExpiry() {
    return tokenService.getRefreshTokenExpiry();
  }

  @Override
  public Instant getPasswordResetTokenExpiry() {
    return tokenService.getPasswordResetTokenExpiry();
  }

  @Override
  public Instant getEmailVerificationTokenExpiry() {
    return tokenService.getEmailVerificationTokenExpiry();
  }

  @Override
  public Instant getOAuthExchangeCodeExpiry() {
    return tokenService.getOAuthExchangeCodeExpiry();
  }
}
