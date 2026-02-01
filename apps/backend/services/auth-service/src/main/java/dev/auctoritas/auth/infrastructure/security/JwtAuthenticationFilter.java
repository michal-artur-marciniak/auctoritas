package dev.auctoritas.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.auctoritas.auth.application.JwtService;
import dev.auctoritas.auth.application.JwtService.JwtValidationResult;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
  * Filter that extracts and validates JWT from Authorization header. Sets the authenticated
  * principal in SecurityContext on successful validation.
  */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      // No token present - let Spring Security handle unauthorized access
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(BEARER_PREFIX.length());
    JwtValidationResult result = jwtService.validateToken(token);

    if (!result.valid()) {
      writeErrorResponse(response, result.errorCode());
      return;
    }

    Claims claims = result.claims();
    Authentication principal;
    try {
      principal = extractPrincipal(claims);
    } catch (IllegalArgumentException | NullPointerException ex) {
      writeErrorResponse(response, "invalid_token_claims");
      return;
    }
    SecurityContextHolder.getContext().setAuthentication(principal);

    filterChain.doFilter(request, response);
  }

  private Authentication extractPrincipal(Claims claims) {
    String endUserId = claims.get(JwtService.CLAIM_END_USER_ID, String.class);
    if (endUserId != null && !endUserId.isBlank()) {
      return extractEndUserPrincipal(claims);
    }
    String orgMemberId = claims.get(JwtService.CLAIM_ORG_MEMBER_ID, String.class);
    if (orgMemberId != null && !orgMemberId.isBlank()) {
      return extractOrganizationMemberPrincipal(claims);
    }
    throw new IllegalArgumentException("Unsupported token claims");
  }

  private EndUserPrincipal extractEndUserPrincipal(Claims claims) {
    UUID endUserId = parseRequiredUuid(claims, JwtService.CLAIM_END_USER_ID);
    UUID projectId = parseRequiredUuid(claims, JwtService.CLAIM_PROJECT_ID);
    String email = requireClaim(claims, JwtService.CLAIM_EMAIL);

    return new EndUserPrincipal(endUserId, projectId, email);
  }

  private OrganizationMemberPrincipal extractOrganizationMemberPrincipal(Claims claims) {
    UUID orgMemberId = parseRequiredUuid(claims, JwtService.CLAIM_ORG_MEMBER_ID);
    UUID orgId = parseRequiredUuid(claims, JwtService.CLAIM_ORG_ID);
    String email = requireClaim(claims, JwtService.CLAIM_EMAIL);
    OrganizationMemberRole role = parseRequiredRole(claims, JwtService.CLAIM_ROLE);

    return new OrganizationMemberPrincipal(orgMemberId, orgId, email, role);
  }

  private String requireClaim(Claims claims, String name) {
    String value = claims.get(name, String.class);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing claim: " + name);
    }
    return value;
  }

  private UUID parseRequiredUuid(Claims claims, String name) {
    return UUID.fromString(requireClaim(claims, name));
  }

  private OrganizationMemberRole parseRequiredRole(Claims claims, String name) {
    String value = requireClaim(claims, name);
    return OrganizationMemberRole.valueOf(value);
  }

  private void writeErrorResponse(HttpServletResponse response, String errorCode)
      throws IOException {
    if (response.isCommitted()) {
      return;
    }
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    objectMapper.writeValue(response.getWriter(), Map.of("error", errorCode));
  }
}
