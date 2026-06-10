package com.aegis.shortener.service;

import com.aegis.shortener.config.AppProperties;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window rate limiter backed by Redis. The first request in a window sets
 * the key expiry; subsequent requests increment until the limit is reached.
 */
@Component
public class RateLimiter {

    private final StringRedisTemplate redis;
    private final int limit;
    private final Duration window;

    public RateLimiter(StringRedisTemplate redis, AppProperties props) {
        this.redis = redis;
        this.limit = props.getRateLimit().getLimit();
        this.window = Duration.ofSeconds(props.getRateLimit().getWindowSeconds());
    }

    public boolean tryAcquire(String clientKey) {
        String key = "ratelimit:" + clientKey;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, window.toSeconds(), TimeUnit.SECONDS);
        }
        return count != null && count <= limit;
    }
}
