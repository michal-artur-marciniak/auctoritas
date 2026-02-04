package dev.auctoritas.auth.adapter.in.web;

import java.util.List;
import java.util.UUID;

public record UserRoleAssignmentResponse(
    List<RoleSummary> roles,
    List<String> permissions) {

  public record RoleSummary(UUID id, String name) {}
}
