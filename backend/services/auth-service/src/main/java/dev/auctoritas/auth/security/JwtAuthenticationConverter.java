package dev.auctoritas.auth.security;

import dev.auctoritas.common.dto.JwtClaims;
import dev.auctoritas.common.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter {

  private static final String ROLE_PREFIX = "ROLE_";

  public AbstractAuthenticationToken convert(JwtClaims claims) {
    if (claims == null) {
      log.debug("Cannot convert null claims to authentication");
      return null;
    }

    JwtPrincipal principal = JwtPrincipal.fromClaims(claims);
    Collection<SimpleGrantedAuthority> authorities = extractAuthorities(claims);

    log.debug("Converted JWT claims for subject '{}' with authorities: {}",
        claims.subject(), authorities);

    return new UsernamePasswordAuthenticationToken(principal, null, authorities);
  }

  private Collection<SimpleGrantedAuthority> extractAuthorities(JwtClaims claims) {
    Collection<SimpleGrantedAuthority> authorities = claims.permissions() != null
        ? claims.permissions().stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList())
        : java.util.Collections.emptyList();

    if (claims.role() != null && !claims.role().isBlank()) {
      authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + claims.role().toUpperCase()));
    }

    return authorities;
  }
}
