package com.aegis.shortener.web.dto;

import com.aegis.shortener.domain.Link;
import java.time.Instant;

public record LinkResponse(
        String code,
        String shortUrl,
        String longUrl,
        Instant createdAt,
        Instant expiresAt) {

    public static LinkResponse from(Link link, String publicBaseUrl) {
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return new LinkResponse(
                link.getCode(),
                base + "/" + link.getCode(),
                link.getLongUrl(),
                link.getCreatedAt(),
                link.getExpiresAt());
    }
}
