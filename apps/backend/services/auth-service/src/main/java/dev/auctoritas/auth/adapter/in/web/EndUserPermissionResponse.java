package dev.auctoritas.auth.adapter.in.web;

import java.util.List;

public record EndUserPermissionResponse(
    List<String> roles,
    List<String> permissions) {}
