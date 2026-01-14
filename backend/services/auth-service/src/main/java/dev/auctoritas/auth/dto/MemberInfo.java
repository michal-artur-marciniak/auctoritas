package dev.auctoritas.auth.dto;

import java.util.UUID;

public record MemberInfo(
    UUID id,
    String email,
    String name,
    String role
) {}
