package com.aegis.shortener.security;

import com.aegis.shortener.config.AppProperties;
import com.aegis.shortener.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies HS256 JSON web tokens. The token subject is the username;
 * the user id and role travel as claims so the auth filter needs no database hit.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirySeconds;

    public JwtService(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirySeconds = props.getJwt().getExpirySeconds();
    }

    public String issue(UUID userId, String username, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(key)
                .compact();
    }

    public AppUserPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AppUserPrincipal(
                UUID.fromString(claims.get("uid", String.class)),
                claims.getSubject(),
                "",
                Role.valueOf(claims.get("role", String.class)));
    }
}
