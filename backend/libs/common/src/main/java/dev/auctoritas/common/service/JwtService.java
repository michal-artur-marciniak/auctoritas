package dev.auctoritas.common.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import dev.auctoritas.common.config.JwtConfig;
import dev.auctoritas.common.dto.JwtClaims;
import dev.auctoritas.common.util.KeyPairUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    private static final String RSA_ALGORITHM = "RSA";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtConfig config;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init() {
        initializeKeys();
        initializeAlgorithm();
    }

    private void initializeKeys() {
        var keyConfig = config.getKeys();
        String privateKeyPath = keyConfig.getPrivateKeyPath();
        String publicKeyPath = keyConfig.getPublicKeyPath();

        if (keyConfig.isAutoGenerate() || !Files.exists(Path.of(privateKeyPath))) {
            KeyPairUtils.initializeDefaultKeys(
                privateKeyPath,
                publicKeyPath,
                keyConfig.getKeySize()
            );
        }

        try {
            this.privateKey = KeyPairUtils.loadPrivateKey(privateKeyPath);
            this.publicKey = KeyPairUtils.loadPublicKey(publicKeyPath);
            log.info("JWT keys loaded successfully from {} and {}", privateKeyPath, publicKeyPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JWT keys", e);
        }
    }

    private void initializeAlgorithm() {
        try {
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
            this.algorithm = Algorithm.RSA256(rsaPublicKey, rsaPrivateKey);
            this.verifier = JWT.require(algorithm).build();
            log.info("JWT algorithm initialized with RS256");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JWT algorithm", e);
        }
    }

    public String generateToken(JwtClaims claims) {
        Instant now = Instant.now();
        Instant expiry = Instant.now().plusSeconds(config.getToken().getAccessTokenTtlSeconds());

        JWTCreator.Builder builder = JWT.create()
            .withSubject(claims.subject())
            .withIssuer(config.getToken().getIssuer())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiry))
            .withKeyId("auctoritas-key-1");

        if (claims.orgId() != null) {
            builder.withClaim("org_id", claims.orgId());
        }
        if (claims.projectId() != null) {
            builder.withClaim("project_id", claims.projectId());
        }
        if (claims.role() != null) {
            builder.withClaim("role", claims.role());
        }
        if (claims.type() != null) {
            builder.withClaim("type", claims.type());
        }
        if (claims.permissions() != null) {
            if (claims.permissions().isEmpty()) {
                builder.withClaim("permissions", "");
            } else {
                builder.withClaim("permissions", String.join(",", claims.permissions()));
            }
        }

        return builder.sign(algorithm);
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String tokenString = token.startsWith(BEARER_PREFIX) ? token.substring(BEARER_PREFIX.length()) : token;

        try {
            verifier.verify(tokenString);
            return true;
        } catch (JWTVerificationException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public JwtClaims extractClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        String tokenString = token.startsWith(BEARER_PREFIX) ? token.substring(BEARER_PREFIX.length()) : token;

        try {
            DecodedJWT decoded = verifier.verify(tokenString);

            String permissionsStr = decoded.getClaim("permissions").asString();
            Set<String> permissions;
            if (permissionsStr == null || permissionsStr.isEmpty()) {
                permissions = Set.of();
            } else {
                permissions = Set.of(permissionsStr.split(","));
                if (permissions.contains("")) {
                    permissions = Set.of();
                }
            }

            return new JwtClaims(
                decoded.getSubject(),
                decoded.getClaim("org_id").asString(),
                decoded.getClaim("project_id").asString(),
                decoded.getClaim("role").asString(),
                decoded.getClaim("type").asString(),
                permissions,
                decoded.getIssuer(),
                decoded.getIssuedAt().getTime() / 1000,
                decoded.getExpiresAt().getTime() / 1000
            );
        } catch (JWTVerificationException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public String extractSubject(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        String tokenString = token.startsWith(BEARER_PREFIX) ? token.substring(BEARER_PREFIX.length()) : token;

        try {
            DecodedJWT decoded = verifier.verify(tokenString);
            return decoded.getSubject();
        } catch (JWTVerificationException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}
