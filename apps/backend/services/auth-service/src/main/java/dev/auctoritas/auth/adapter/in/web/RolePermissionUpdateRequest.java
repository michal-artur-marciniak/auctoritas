package dev.auctoritas.auth.adapter.in.web;

import java.util.List;

public record RolePermissionUpdateRequest(List<String> permissionCodes) {}
