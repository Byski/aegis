package com.aegis.shortener.service;

import com.aegis.shortener.domain.Link;
import com.aegis.shortener.repository.LinkRepository;
import com.aegis.shortener.web.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {

    private static final String CACHE_PREFIX = "link:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final LinkRepository links;
    private final ShortCodeGenerator codeGenerator;
    private final StringRedisTemplate redis;

    public LinkService(LinkRepository links, ShortCodeGenerator codeGenerator, StringRedisTemplate redis) {
        this.links = links;
        this.codeGenerator = codeGenerator;
        this.redis = redis;
    }

    @Transactional
    public Link create(UUID ownerId, String longUrl, String customCode, Long ttlSeconds) {
        Instant expiresAt = ttlSeconds == null ? null : Instant.now().plusSeconds(ttlSeconds);
        String code = customCode != null ? claimCustomCode(customCode) : generateUniqueCode();
        Link link = links.save(new Link(code, longUrl, ownerId, expiresAt));
        cachePut(link);
        return link;
    }

    private String claimCustomCode(String code) {
        if (links.existsByCode(code)) {
            throw ApiException.conflict("Code already in use");
        }
        return code;
    }

    private String generateUniqueCode() {
        for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
            String candidate = codeGenerator.generate();
            if (!links.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw ApiException.conflict("Could not allocate a unique code, retry");
    }

    /** Resolves a code to its target URL for redirecting. Checks cache first. */
    @Transactional(readOnly = true)
    public String resolve(String code) {
        String cached = redis.opsForValue().get(CACHE_PREFIX + code);
        if (cached != null) {
            return cached;
        }
        Link link = links.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("Unknown code"));
        if (link.isExpired(Instant.now())) {
            throw ApiException.notFound("Link expired");
        }
        cachePut(link);
        return link.getLongUrl();
    }

    @Transactional(readOnly = true)
    public Link requireOwned(String code, UUID requesterId, boolean isAdmin) {
        Link link = links.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("Unknown code"));
        if (!isAdmin && !link.getOwnerId().equals(requesterId)) {
            throw ApiException.forbidden("Not the owner of this link");
        }
        return link;
    }

    private void cachePut(Link link) {
        long ttl = CACHE_TTL.toSeconds();
        if (link.getExpiresAt() != null) {
            long untilExpiry = Math.max(1, link.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
            ttl = Math.min(ttl, untilExpiry);
        }
        redis.opsForValue().set(CACHE_PREFIX + link.getCode(), link.getLongUrl(), ttl, TimeUnit.SECONDS);
    }

    public Optional<Link> findByCode(String code) {
        return links.findByCode(code);
    }
}
