package com.example.api.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration with JWT-based stateless authentication.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OrgJwtAuthenticationFilter orgJwtAuthenticationFilter;
    private final PlatformAdminJwtAuthenticationFilter platformAdminJwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oAuthSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuthFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final FrontendCorsProperties frontendCorsProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          OrgJwtAuthenticationFilter orgJwtAuthenticationFilter,
                          PlatformAdminJwtAuthenticationFilter platformAdminJwtAuthenticationFilter,
                          ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                          OAuth2AuthenticationSuccessHandler oAuthSuccessHandler,
                          OAuth2AuthenticationFailureHandler oAuthFailureHandler,
                          HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
                          FrontendCorsProperties frontendCorsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.orgJwtAuthenticationFilter = orgJwtAuthenticationFilter;
        this.platformAdminJwtAuthenticationFilter = platformAdminJwtAuthenticationFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.oAuthSuccessHandler = oAuthSuccessHandler;
        this.oAuthFailureHandler = oAuthFailureHandler;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.frontendCorsProperties = frontendCorsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/platform/auth/**").permitAll()
                .requestMatchers("/api/v1/customers/orgs/register").permitAll()
                .requestMatchers("/api/v1/customers/auth/login").permitAll()
                .requestMatchers("/api/v1/customers/orgs/*/members/accept").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/hello").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(authorization ->
                                authorization.authorizationRequestRepository(authorizationRequestRepository))
                        .successHandler(oAuthSuccessHandler)
                        .failureHandler(oAuthFailureHandler)
                )
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(orgJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(platformAdminJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final var config = new CorsConfiguration();
        final var allowedOrigins = frontendCorsProperties.allowedOrigins();
        if (allowedOrigins.isEmpty()) {
            return new UrlBasedCorsConfigurationSource();
        }
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-API-Key"));
        config.setAllowCredentials(true);

        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
