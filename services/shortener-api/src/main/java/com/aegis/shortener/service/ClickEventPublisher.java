package com.aegis.shortener.service;

import com.aegis.shortener.config.AppProperties;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes redirect click events onto a Redis stream. The analytics service
 * consumes this stream. Publishing failures never block the redirect path.
 */
@Component
public class ClickEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClickEventPublisher.class);

    private final StringRedisTemplate redis;
    private final String stream;

    public ClickEventPublisher(StringRedisTemplate redis, AppProperties props) {
        this.redis = redis;
        this.stream = props.getClickStream();
    }

    public void publish(String code, String referer, String userAgent) {
        Map<String, String> fields = new HashMap<>();
        fields.put("code", code);
        fields.put("ts", Instant.now().toString());
        fields.put("referer", referer == null ? "" : referer);
        fields.put("user_agent", userAgent == null ? "" : userAgent);
        try {
            redis.opsForStream().add(StreamRecords.newRecord().ofMap(fields).withStreamKey(stream));
        } catch (Exception ex) {
            log.warn("Failed to publish click event for code {}", code, ex);
        }
    }
}
