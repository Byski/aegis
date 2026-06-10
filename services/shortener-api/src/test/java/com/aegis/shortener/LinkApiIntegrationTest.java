package com.aegis.shortener;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

class LinkApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @BeforeEach
    void disableRedirectFollowing() {
        // Assert the 302 itself rather than chasing it to the target host.
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory(client));
    }

    @Test
    void fullFlow_register_login_create_redirect() {
        String username = "alice-" + System.nanoTime();

        ResponseEntity<Void> register = post("/api/v1/auth/register",
                Map.of("username", username, "password", "password123"), null, Void.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> login = post("/api/v1/auth/login",
                Map.of("username", username, "password", "password123"), null, Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) login.getBody().get("token");
        assertThat(token).isNotBlank();

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> create = post("/api/v1/links",
                Map.of("longUrl", "https://example.com/some/long/path"), token, Map.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String code = (String) create.getBody().get("code");
        assertThat(code).isNotBlank();

        ResponseEntity<Void> redirect = rest.exchange("/" + code, HttpMethod.GET,
                HttpEntity.EMPTY, Void.class);
        assertThat(redirect.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(redirect.getHeaders().getLocation().toString())
                .isEqualTo("https://example.com/some/long/path");
    }

    @Test
    void createLink_withoutToken_isUnauthorized() {
        ResponseEntity<Void> create = post("/api/v1/links",
                Map.of("longUrl", "https://example.com"), null, Void.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createLink_withInvalidUrl_isBadRequest() {
        String username = "bob-" + System.nanoTime();
        post("/api/v1/auth/register",
                Map.of("username", username, "password", "password123"), null, Void.class);
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> login = post("/api/v1/auth/login",
                Map.of("username", username, "password", "password123"), null, Map.class);
        String token = (String) login.getBody().get("token");

        ResponseEntity<Void> create = post("/api/v1/links",
                Map.of("longUrl", "not-a-url"), token, Void.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void redirect_unknownCode_isNotFound() {
        ResponseEntity<Void> redirect = rest.exchange("/doesnotexist", HttpMethod.GET,
                HttpEntity.EMPTY, Void.class);
        assertThat(redirect.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private <T> ResponseEntity<T> post(String path, Object body, String token, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.postForEntity(path, new HttpEntity<>(body, headers), type);
    }
}
