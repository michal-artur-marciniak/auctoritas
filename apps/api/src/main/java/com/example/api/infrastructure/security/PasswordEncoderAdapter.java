package com.example.api.infrastructure.security;

import com.example.api.domain.user.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt adapter implementing the domain {@link PasswordEncoder} port.
 */
@Component
public class PasswordEncoderAdapter implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt;

    public PasswordEncoderAdapter(BCryptPasswordEncoder bcrypt) {
        this.bcrypt = bcrypt;
    }

    @Override
    public String encode(String rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return bcrypt.matches(rawPassword, encodedPassword);
    }

}
