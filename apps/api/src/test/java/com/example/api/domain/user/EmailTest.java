package com.example.api.domain.user;

import com.example.api.domain.user.exception.InvalidEmailException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

    @Test
    void normalizesAndValidates() {
        final var email = new Email("  USER@Example.com ");
        assertEquals("user@example.com", email.value());
    }

    @Test
    void rejectsInvalidEmail() {
        assertThrows(InvalidEmailException.class, () -> new Email("invalid"));
    }
}
