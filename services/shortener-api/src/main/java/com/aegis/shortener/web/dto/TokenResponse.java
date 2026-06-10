package com.aegis.shortener.web.dto;

public record TokenResponse(String token, String tokenType, long expiresInSeconds) {
}
