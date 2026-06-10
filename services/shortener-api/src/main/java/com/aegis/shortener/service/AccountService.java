package com.aegis.shortener.service;

import com.aegis.shortener.domain.AppUser;
import com.aegis.shortener.domain.Role;
import com.aegis.shortener.repository.AppUserRepository;
import com.aegis.shortener.web.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AccountService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public AppUser register(String username, String rawPassword) {
        if (users.existsByUsername(username)) {
            throw ApiException.conflict("Username already taken");
        }
        return users.save(new AppUser(username, encoder.encode(rawPassword), Role.USER));
    }
}
