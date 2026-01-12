package dev.auctoritas.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuthTokensTest {
  @Test
  void testDefaults() {
    AuthTokens tokens = new AuthTokens("access", "refresh", 1800);
    assertEquals("Bearer", tokens.tokenType());
  }

  @Test
  void testAllArgsConstructor() {
    AuthTokens tokens = new AuthTokens("access", "refresh", 1800, "Token");
    assertEquals("Token", tokens.tokenType());
  }
}
