package com.example.api.infrastructure.security;

import com.example.api.domain.organization.OrganizationMemberId;
import com.example.api.domain.organization.OrganizationMemberRepository;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT authentication filter for organization member endpoints.
 */
@Component
public class OrgJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final OrganizationMemberRepository memberRepository;
    private final String secret;
    private final String cookieName;

    public OrgJwtAuthenticationFilter(OrganizationMemberRepository memberRepository,
                                      @Value("${jwt.secret}") String secret,
                                      @Value("${app.org.auth.cookie-name:org_access_token}") String cookieName) {
        this.memberRepository = memberRepository;
        this.secret = secret;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            final var token = extractToken(request);
            if (token != null) {
                try {
                    final var claims = Jwts.parser()
                            .verifyWith(secretKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                    if ("org".equals(claims.get("type"))
                            && claims.getExpiration() != null
                            && claims.getExpiration().after(new Date())) {
                        final var memberId = claims.getSubject();
                        memberRepository.findById(OrganizationMemberId.of(memberId)).ifPresent(member -> {
                            final var authority = new SimpleGrantedAuthority("ROLE_" + member.getRole().name());
                            final var authentication = new UsernamePasswordAuthenticationToken(
                                    memberId, null, List.of(authority)
                            );
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });
                    }
                } catch (JwtException | IllegalArgumentException ignored) {
                    // Invalid token, continue without authentication
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        final var header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        if (cookieName == null || cookieName.isBlank()) {
            return null;
        }
        if (request.getCookies() == null) {
            return null;
        }
        for (final var cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final var path = request.getRequestURI();
        // Only filter org endpoints
        return !path.startsWith("/api/v1/org/");
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
