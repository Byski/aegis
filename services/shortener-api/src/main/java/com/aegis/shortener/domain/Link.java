package com.aegis.shortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "link")
public class Link {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected Link() {
        // required by JPA
    }

    public Link(String code, String longUrl, UUID ownerId, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.longUrl = longUrl;
        this.ownerId = ownerId;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
