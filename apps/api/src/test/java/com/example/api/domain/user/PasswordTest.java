package com.example.api.domain.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class PasswordTest {

    @Test
    void rejectsShortPassword() {
        final var encoder = mock(PasswordEncoder.class);
        assertThrows(IllegalArgumentException.class, () -> Password.create("short", encoder));
    }
}
