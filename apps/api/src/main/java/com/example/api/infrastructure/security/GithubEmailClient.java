package com.example.api.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * Fetches verified primary email for GitHub OAuth users.
 */
@Component
public class GithubEmailClient {

    private static final Logger log = LoggerFactory.getLogger(GithubEmailClient.class);
    private static final String EMAILS_PATH = "/user/emails";

    private final RestClient restClient;

    public GithubEmailClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.USER_AGENT, "openagents-cloud")
                .build();
    }

    public Optional<String> fetchPrimaryEmail(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }

        try {
            final var emails = restClient.get()
                    .uri(EMAILS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(EmailRecord[].class);

            if (emails == null) {
                return Optional.empty();
            }

            return List.of(emails).stream()
                    .filter(EmailRecord::primary)
                    .filter(EmailRecord::verified)
                    .map(EmailRecord::email)
                    .findFirst();
        } catch (RuntimeException ex) {
            log.warn("GitHub email lookup failed", ex);
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmailRecord(String email, boolean primary, boolean verified) {
    }
}
