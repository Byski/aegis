package com.aegis.shortener.config;

import com.aegis.shortener.domain.AppUser;
import com.aegis.shortener.domain.Role;
import com.aegis.shortener.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates an ADMIN account on startup when admin credentials are supplied via
 * the environment and the account does not already exist. No-op otherwise.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminSeeder(AppUserRepository users, PasswordEncoder encoder,
                       @Value("${admin.username:}") String username,
                       @Value("${admin.password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (username.isBlank() || password.isBlank()) {
            return;
        }
        if (users.existsByUsername(username)) {
            return;
        }
        users.save(new AppUser(username, encoder.encode(password), Role.ADMIN));
        log.info("Seeded admin account '{}'", username);
    }
}
