package dev.auctoritas.auth.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for MFA encryption.
 */
@ConfigurationProperties(prefix = "auctoritas.auth.mfa.encryption")
@Validated
public record MfaEncryptionProperties(
    @NotBlank
    @Size(min = 44, max = 44) String key) {
}
