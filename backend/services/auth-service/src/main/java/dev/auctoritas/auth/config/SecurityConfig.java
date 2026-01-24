package dev.auctoritas.auth.config;

import dev.auctoritas.auth.security.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    "/actuator/health",
    "/.well-known/jwks.json",
    "/.well-known/openid-configuration"
  };

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
                    .permitAll())
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
}
