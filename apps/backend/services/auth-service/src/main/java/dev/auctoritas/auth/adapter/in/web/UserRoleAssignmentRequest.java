package dev.auctoritas.auth.adapter.in.web;

import java.util.List;
import java.util.UUID;

public record UserRoleAssignmentRequest(List<UUID> roleIds) {}
