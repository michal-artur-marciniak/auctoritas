package com.example.api.application.organization;

import java.util.UUID;

/**
 * Utility for generating invitation tokens.
 */
final class MemberTokenGenerator {

    private MemberTokenGenerator() {
        // Utility class
    }

    static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
