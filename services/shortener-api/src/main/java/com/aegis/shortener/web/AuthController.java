package com.aegis.shortener.web;

import com.aegis.shortener.config.AppProperties;
import com.aegis.shortener.security.AppUserPrincipal;
import com.aegis.shortener.security.JwtService;
import com.aegis.shortener.service.AccountService;
import com.aegis.shortener.web.dto.LoginRequest;
import com.aegis.shortener.web.dto.RegisterRequest;
import com.aegis.shortener.web.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AccountService accounts;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final long expirySeconds;

    public AuthController(AccountService accounts, AuthenticationManager authManager,
                          JwtService jwtService, AppProperties props) {
        this.accounts = accounts;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.expirySeconds = props.getJwt().getExpirySeconds();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        accounts.register(request.username(), request.password());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        String token = jwtService.issue(principal.getId(), principal.getUsername(), principal.getRole());
        return new TokenResponse(token, "Bearer", expirySeconds);
    }
}
