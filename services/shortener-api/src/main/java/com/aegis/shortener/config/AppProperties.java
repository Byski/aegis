package com.aegis.shortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized application settings. Values come from environment variables in
 * every deployment; defaults here are for local development only.
 */
@ConfigurationProperties(prefix = "aegis")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final RateLimit rateLimit = new RateLimit();
    /** Public base used when rendering short URLs back to clients. */
    private String publicBaseUrl = "http://localhost:8080";
    /** Redis stream that receives click events for the analytics service. */
    private String clickStream = "clicks";

    public Jwt getJwt() {
        return jwt;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getClickStream() {
        return clickStream;
    }

    public void setClickStream(String clickStream) {
        this.clickStream = clickStream;
    }

    public static class Jwt {
        /** HMAC signing secret. Must be at least 32 bytes. Set via environment. */
        private String secret = "local-development-secret-change-me-please-32b"; // hygiene:allow-secret development default, overridden by env
        /** Token lifetime in seconds. */
        private long expirySeconds = 3600;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirySeconds() {
            return expirySeconds;
        }

        public void setExpirySeconds(long expirySeconds) {
            this.expirySeconds = expirySeconds;
        }
    }

    public static class RateLimit {
        /** Requests allowed per window, per client key. */
        private int limit = 100;
        /** Window length in seconds. */
        private int windowSeconds = 60;

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
