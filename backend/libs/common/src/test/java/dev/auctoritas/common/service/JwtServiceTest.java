package dev.auctoritas.common.service;

import dev.auctoritas.common.config.JwtConfig;
import dev.auctoritas.common.dto.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @TempDir
    Path tempDir;

    private JwtService jwtService;
    private JwtConfig config;

    @BeforeEach
    void setUp() {
        config = new JwtConfig();
        config.setKeys(new JwtConfig.KeyConfig());
        config.setToken(new JwtConfig.TokenConfig());

        config.getKeys().setPrivateKeyPath(tempDir.resolve("private.pem").toString());
        config.getKeys().setPublicKeyPath(tempDir.resolve("public.pem").toString());
        config.getKeys().setKeySize(2048);
        config.getKeys().setAutoGenerate(true);

        config.getToken().setAccessTokenTtlSeconds(1800);
        config.getToken().setRefreshTokenTtlSeconds(604800);
        config.getToken().setIssuer("auctoritas.dev");

        jwtService = new JwtService(config);
        jwtService.init();
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGeneration {

        @Test
        @DisplayName("should generate valid JWT with all claims")
        void shouldGenerateValidJwtWithAllClaims() {
            JwtClaims claims = new JwtClaims(
                "user-123",
                "org-456",
                "project-789",
                "OWNER",
                "org_member",
                Set.of("read", "write"),
                "auctoritas.dev",
                Instant.now().getEpochSecond(),
                Instant.now().plusSeconds(1800).getEpochSecond()
            );

            String token = jwtService.generateToken(claims);

            assertNotNull(token);
            assertFalse(token.isBlank());
            assertTrue(token.contains("."));
        }

        @Test
        @DisplayName("should generate JWT with minimal claims")
        void shouldGenerateJwtWithMinimalClaims() {
            JwtClaims claims = new JwtClaims(
                "user-123",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0
            );

            String token = jwtService.generateToken(claims);

            assertNotNull(token);
            assertTrue(jwtService.validateToken(token));
        }

        @Test
        @DisplayName("should generate JWT with empty permissions set")
        void shouldGenerateJwtWithEmptyPermissions() {
            JwtClaims claims = new JwtClaims(
                "user-123",
                "org-456",
                null,
                "MEMBER",
                "org_member",
                Set.of(),
                "auctoritas.dev",
                Instant.now().getEpochSecond(),
                Instant.now().plusSeconds(1800).getEpochSecond()
            );

            String token = jwtService.generateToken(claims);

            assertNotNull(token);
            JwtClaims extracted = jwtService.extractClaims(token);
            assertNotNull(extracted.permissions());
            assertTrue(extracted.permissions().isEmpty());
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("should return true for valid token")
        void shouldReturnTrueForValidToken() {
            JwtClaims claims = new JwtClaims(
                "user-123", "org-456", null, "OWNER", "org_member",
                Set.of("read"), "auctoritas.dev", 0, 0
            );
            String token = jwtService.generateToken(claims);

            assertTrue(jwtService.validateToken(token));
        }

        @Test
        @DisplayName("should return false for null token")
        void shouldReturnFalseForNullToken() {
            assertFalse(jwtService.validateToken(null));
        }

        @Test
        @DisplayName("should return false for blank token")
        void shouldReturnFalseForBlankToken() {
            assertFalse(jwtService.validateToken("   "));
        }

        @Test
        @DisplayName("should return false for empty string token")
        void shouldReturnFalseForEmptyStringToken() {
            assertFalse(jwtService.validateToken(""));
        }

        @Test
        @DisplayName("should return false for tampered token")
        void shouldReturnFalseForTamperedToken() {
            JwtClaims claims = new JwtClaims(
                "user-123", "org-456", null, "OWNER", "org_member",
                Set.of("read"), "auctoritas.dev", 0, 0
            );
            String token = jwtService.generateToken(claims);
            String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

            assertFalse(jwtService.validateToken(tamperedToken));
        }

        @Test
        @DisplayName("should return false for invalid JWT format")
        void shouldReturnFalseForInvalidJwtFormat() {
            assertFalse(jwtService.validateToken("not.a.valid.jwt.token"));
        }

        @Test
        @DisplayName("should return true for Bearer prefix token")
        void shouldReturnTrueForBearerPrefixToken() {
            JwtClaims claims = new JwtClaims(
                "user-123", "org-456", null, "OWNER", "org_member",
                Set.of("read"), "auctoritas.dev", 0, 0
            );
            String token = jwtService.generateToken(claims);
            String bearerToken = "Bearer " + token;

            assertTrue(jwtService.validateToken(bearerToken));
        }
    }

    @Nested
    @DisplayName("Claims Extraction")
    class ClaimsExtraction {

        @Test
        @DisplayName("should extract all claims correctly")
        void shouldExtractAllClaimsCorrectly() {
            JwtClaims originalClaims = new JwtClaims(
                "user-123",
                "org-456",
                "project-789",
                "ADMIN",
                "org_member",
                Set.of("read", "write", "delete"),
                "auctoritas.dev",
                Instant.now().getEpochSecond(),
                Instant.now().plusSeconds(1800).getEpochSecond()
            );

            String token = jwtService.generateToken(originalClaims);
            JwtClaims extractedClaims = jwtService.extractClaims(token);

            assertEquals("user-123", extractedClaims.subject());
            assertEquals("org-456", extractedClaims.orgId());
            assertEquals("project-789", extractedClaims.projectId());
            assertEquals("ADMIN", extractedClaims.role());
            assertEquals("org_member", extractedClaims.type());
            assertEquals("auctoritas.dev", extractedClaims.issuer());
            assertEquals(Set.of("read", "write", "delete"), extractedClaims.permissions());
        }

        @Test
        @DisplayName("should extract subject correctly")
        void shouldExtractSubjectCorrectly() {
            JwtClaims claims = new JwtClaims(
                "user-123", "org-456", null, "OWNER", "org_member",
                Set.of("read"), "auctoritas.dev", 0, 0
            );
            String token = jwtService.generateToken(claims);

            assertEquals("user-123", jwtService.extractSubject(token));
        }

        @Test
        @DisplayName("should throw exception for null token when extracting claims")
        void shouldThrowExceptionForNullTokenWhenExtractingClaims() {
            assertThrows(IllegalArgumentException.class, () -> jwtService.extractClaims(null));
        }

        @Test
        @DisplayName("should throw exception for blank token when extracting claims")
        void shouldThrowExceptionForBlankTokenWhenExtractingClaims() {
            assertThrows(IllegalArgumentException.class, () -> jwtService.extractClaims("   "));
        }

        @Test
        @DisplayName("should throw exception for invalid token when extracting claims")
        void shouldThrowExceptionForInvalidTokenWhenExtractingClaims() {
            assertThrows(IllegalArgumentException.class, () -> jwtService.extractClaims("invalid.token.here"));
        }

        @Test
        @DisplayName("should handle Bearer prefix when extracting claims")
        void shouldHandleBearerPrefixWhenExtractingClaims() {
            JwtClaims claims = new JwtClaims(
                "user-123", "org-456", null, "OWNER", "org_member",
                Set.of("read"), "auctoritas.dev", 0, 0
            );
            String token = jwtService.generateToken(claims);
            String bearerToken = "Bearer " + token;

            JwtClaims extracted = jwtService.extractClaims(bearerToken);
            assertEquals("user-123", extracted.subject());
        }

        @Test
        @DisplayName("should extract single permission correctly")
        void shouldExtractSinglePermissionCorrectly() {
            JwtClaims claims = new JwtClaims(
                "user-123", "org-456", null, "OWNER", "org_member",
                Set.of("read"), "auctoritas.dev", 0, 0
            );
            String token = jwtService.generateToken(claims);
            JwtClaims extracted = jwtService.extractClaims(token);

            assertEquals(1, extracted.permissions().size());
            assertTrue(extracted.permissions().contains("read"));
        }
    }
}
