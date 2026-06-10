package com.aegis.shortener.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateLinkRequest(
        @NotBlank @URL @Size(max = 2048) String longUrl,
        @Pattern(regexp = "[A-Za-z0-9_-]{3,32}", message = "code must be 3-32 url-safe characters")
        String customCode,
        @Min(1) Long ttlSeconds) {
}
