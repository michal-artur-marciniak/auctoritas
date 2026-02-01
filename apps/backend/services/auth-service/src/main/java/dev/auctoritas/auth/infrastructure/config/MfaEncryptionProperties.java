package dev.auctoritas.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MFA encryption.
 */
@ConfigurationProperties(prefix = "auctoritas.auth.mfa.encryption")
public record MfaEncryptionProperties(String key) {
}
