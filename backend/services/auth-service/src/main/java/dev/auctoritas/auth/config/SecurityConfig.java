package dev.auctoritas.auth.config;

import dev.auctoritas.auth.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  /** Public endpoints that don't require JWT authentication */
  private static final String[] PUBLIC_ENDPOINTS = {
    "/api/v1/org/register",
    "/api/v1/org/auth/login",
    "/api/v1/org/auth/refresh",
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/auth/refresh",
    "/api/v1/auth/oauth/exchange",
    "/api/v1/auth/password/forgot",
    "/api/v1/auth/password/reset",
    "/api/v1/auth/register/verify-email",
    "/api/v1/auth/register/resend-verification",
    "/internal/api-keys/resolve",
    "/internal/oauth/google/authorize",
    "/internal/oauth/google/callback",
    "/internal/oauth/github/authorize",
    "/internal/oauth/github/callback",
    "/internal/oauth/microsoft/authorize",
    "/internal/oauth/microsoft/callback",
    "/internal/oauth/facebook/authorize",
    "/internal/oauth/facebook/callback",
    "/actuator/health",
    "/.well-known/jwks.json",
    "/.well-known/openid-configuration"
  };

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(PUBLIC_ENDPOINTS)
                    .permitAll()
                    .requestMatchers("/api/v1/org/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, authException) ->
                            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized"))
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "insufficient_role")))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new Argon2PasswordEncoder(
        16, // salt length bytes
        32, // hash length bytes
        4, // parallelism
        65536, // memory in KB (64MB)
        3 // iterations
        );
  }

  private void writeErrorResponse(HttpServletResponse response, int status, String errorCode)
      throws IOException {
    if (response.isCommitted()) {
      return;
    }
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(status);
    objectMapper.writeValue(response.getWriter(), Map.of("error", errorCode));
  }
}
