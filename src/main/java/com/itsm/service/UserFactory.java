package com.itsm.service;

import com.itsm.domain.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Factory pattern: creates the right {@link User} subclass for a role and always hashes
 * the password, so no caller can accidentally store a plain-text password.
 */
@Component
public class UserFactory {

    private final PasswordEncoder passwordEncoder;

    public UserFactory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public User create(Role role, String name, String email, String rawPassword, String detail) {
        String hash = passwordEncoder.encode(rawPassword);
        return switch (role) {
            case EMPLOYEE -> new Employee(name, email, hash, detail);
            case TECHNICIAN -> new Technician(name, email, hash, 5, 1);
            case ADMIN -> new Administrator(name, email, hash, detail == null || detail.isBlank() ? "IT Manager" : detail);
        };
    }

    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
