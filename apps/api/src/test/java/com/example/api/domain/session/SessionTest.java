package com.example.api.domain.session;

import com.example.api.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {

    @Test
    void marksSessionInactiveWhenExpiredOrRevoked() {
        final var now = Instant.now();
        final var session = Session.create(UserId.of("user-id"), now.plusSeconds(60));

        assertTrue(session.isActive(now));

        session.revoke();
        assertFalse(session.isActive(now));

        final var expired = Session.create(UserId.of("user-id"), now.minusSeconds(60));
        assertFalse(expired.isActive(now));
    }
}
