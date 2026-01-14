package dev.auctoritas.auth.security;

import dev.auctoritas.common.dto.JwtClaims;
import dev.auctoritas.common.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    String token = extractToken(request);

    if (token != null && jwtService.validateToken(token)) {
      try {
        JwtClaims claims = jwtService.extractClaims(token);
        Authentication authentication = jwtAuthenticationConverter.convert(claims);

        if (authentication != null) {
          SecurityContextHolder.getContext().setAuthentication(authentication);
          log.debug("Successfully authenticated subject: {} with orgId: {}",
              claims.subject(), claims.orgId());
        }
      } catch (Exception e) {
        log.warn("Failed to process JWT token: {}", e.getMessage());
        SecurityContextHolder.clearContext();
      }
    }

    filterChain.doFilter(request, response);
  }

  private String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.startsWith("/actuator/")
        || path.startsWith("/api/v1/org/register")
        || path.startsWith("/api/v1/org/check-slug")
        || path.startsWith("/api/v1/org/auth/login")
        || path.startsWith("/api/v1/org/auth/refresh")
        || path.startsWith("/api/v1/org/auth/mfa/verify");
  }
}
