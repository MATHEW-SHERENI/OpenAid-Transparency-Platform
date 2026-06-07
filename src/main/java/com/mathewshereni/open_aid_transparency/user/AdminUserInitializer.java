package com.mathewshereni.open_aid_transparency.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstraps a default ADMIN account on startup if one doesn't already exist,
 * so the very first admin can log in. Credentials come from config/env with
 * dev defaults - the password MUST be overridden in production.
 *
 * This solves the chicken-and-egg problem: creating users is ADMIN-only, so we
 * need at least one admin to exist before anyone can log in as admin.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.email:admin@openaid.local}")
    private String adminEmail;

    @Value("${app.admin.password:admin12345}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (repository.existsByUsernameIgnoreCase(adminUsername)) {
            return;
        }
        AppUser admin = AppUser.builder()
                .username(adminUsername)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();
        repository.save(admin);
        log.warn("Seeded default ADMIN user '{}' - CHANGE THE PASSWORD in production!", adminUsername);
    }
}
