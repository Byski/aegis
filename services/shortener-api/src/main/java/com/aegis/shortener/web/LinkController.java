package com.aegis.shortener.web;

import com.aegis.shortener.config.AppProperties;
import com.aegis.shortener.domain.Link;
import com.aegis.shortener.security.AppUserPrincipal;
import com.aegis.shortener.service.LinkService;
import com.aegis.shortener.service.RateLimiter;
import com.aegis.shortener.web.dto.CreateLinkRequest;
import com.aegis.shortener.web.dto.LinkResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
public class LinkController {

    private final LinkService linkService;
    private final RateLimiter rateLimiter;
    private final String publicBaseUrl;

    public LinkController(LinkService linkService, RateLimiter rateLimiter, AppProperties props) {
        this.linkService = linkService;
        this.rateLimiter = rateLimiter;
        this.publicBaseUrl = props.getPublicBaseUrl();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse create(@Valid @RequestBody CreateLinkRequest request,
                               @AuthenticationPrincipal AppUserPrincipal principal) {
        if (!rateLimiter.tryAcquire("create:" + principal.getId())) {
            throw ApiException.tooManyRequests("Rate limit exceeded");
        }
        Link link = linkService.create(
                principal.getId(), request.longUrl(), request.customCode(), request.ttlSeconds());
        return LinkResponse.from(link, publicBaseUrl);
    }

    @GetMapping("/{code}")
    public LinkResponse get(@PathVariable String code,
                            @AuthenticationPrincipal AppUserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Link link = linkService.requireOwned(code, principal.getId(), isAdmin);
        return LinkResponse.from(link, publicBaseUrl);
    }
}
