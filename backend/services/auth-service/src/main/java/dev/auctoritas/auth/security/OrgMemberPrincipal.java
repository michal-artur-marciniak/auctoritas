package dev.auctoritas.auth.security;

import dev.auctoritas.auth.shared.enums.OrgMemberRole;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Represents the authenticated org member in Spring Security context. This is used as the
 * Authentication object in SecurityContextHolder.
 */
public record OrgMemberPrincipal(UUID orgMemberId, UUID orgId, String email, OrgMemberRole role)
    implements Authentication {

  public OrgMemberPrincipal {
    Objects.requireNonNull(orgMemberId, "orgMemberId");
    Objects.requireNonNull(orgId, "orgId");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(role, "role");
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public Object getCredentials() {
    return null; // JWT already validated, no credentials stored
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return this;
  }

  @Override
  public boolean isAuthenticated() {
    return true;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    if (!isAuthenticated) {
      throw new IllegalArgumentException("Cannot un-authenticate org member principal");
    }
  }

  @Override
  public String getName() {
    return email;
  }
}
