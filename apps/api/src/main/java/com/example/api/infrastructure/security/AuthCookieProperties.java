package com.example.api.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Configuration properties for auth cookies.
 */
@Component
@ConfigurationProperties(prefix = "app.auth.cookies")
public class AuthCookieProperties {

    private String domain = "";
    private boolean secure = true;
    private String sameSite = "Lax";
    private int accessMaxAgeSeconds = 86400;
    private int refreshMaxAgeSeconds = 604800;
    private String accessTokenName = "access_token";
    private String refreshTokenName = "refresh_token";

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = Objects.requireNonNullElse(domain, "");
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = Objects.requireNonNullElse(sameSite, "Lax");
    }

    public int getAccessMaxAgeSeconds() {
        return accessMaxAgeSeconds;
    }

    public void setAccessMaxAgeSeconds(int accessMaxAgeSeconds) {
        this.accessMaxAgeSeconds = accessMaxAgeSeconds;
    }

    public int getRefreshMaxAgeSeconds() {
        return refreshMaxAgeSeconds;
    }

    public void setRefreshMaxAgeSeconds(int refreshMaxAgeSeconds) {
        this.refreshMaxAgeSeconds = refreshMaxAgeSeconds;
    }

    public String getAccessTokenName() {
        return accessTokenName;
    }

    public void setAccessTokenName(String accessTokenName) {
        this.accessTokenName = Objects.requireNonNullElse(accessTokenName, "access_token");
    }

    public String getRefreshTokenName() {
        return refreshTokenName;
    }

    public void setRefreshTokenName(String refreshTokenName) {
        this.refreshTokenName = Objects.requireNonNullElse(refreshTokenName, "refresh_token");
    }
}
