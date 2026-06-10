package com.aegis.shortener.security;

import com.aegis.shortener.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsername(username)
                .map(u -> new AppUserPrincipal(u.getId(), u.getUsername(), u.getPasswordHash(), u.getRole()))
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
