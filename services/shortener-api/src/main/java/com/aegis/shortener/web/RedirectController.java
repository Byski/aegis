package com.aegis.shortener.web;

import com.aegis.shortener.service.ClickEventPublisher;
import com.aegis.shortener.service.LinkService;
import com.aegis.shortener.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final LinkService linkService;
    private final ClickEventPublisher clickPublisher;
    private final RateLimiter rateLimiter;

    public RedirectController(LinkService linkService, ClickEventPublisher clickPublisher,
                              RateLimiter rateLimiter) {
        this.linkService = linkService;
        this.clickPublisher = clickPublisher;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/{code:[A-Za-z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        if (!rateLimiter.tryAcquire("redirect:" + clientIp(request))) {
            throw ApiException.tooManyRequests("Rate limit exceeded");
        }
        String target = linkService.resolve(code);
        clickPublisher.publish(code, request.getHeader("Referer"), request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
