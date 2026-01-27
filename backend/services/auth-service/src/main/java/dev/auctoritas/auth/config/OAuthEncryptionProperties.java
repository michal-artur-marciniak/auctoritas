package dev.auctoritas.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auctoritas.auth.oauth.encryption")
public record OAuthEncryptionProperties(String key, String salt) {}
