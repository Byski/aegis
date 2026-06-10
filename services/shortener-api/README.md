# shortener-api

Java 21 + Spring Boot service. Creates short links behind JWT auth, redirects
public traffic, caches lookups in Redis, and publishes click events to a Redis
stream for the analytics service to consume.

## Endpoints

| Method | Path                   | Auth        | Purpose                                  |
|--------|------------------------|-------------|------------------------------------------|
| POST   | /api/v1/auth/register  | none        | Create a USER account                    |
| POST   | /api/v1/auth/login     | none        | Exchange credentials for a JWT           |
| POST   | /api/v1/links          | bearer JWT  | Create a short link                      |
| GET    | /api/v1/links/{code}   | bearer JWT  | Fetch a link the caller owns (ADMIN: any) |
| GET    | /{code}                | none        | Redirect (302) and emit a click event    |
| GET    | /actuator/health/*     | none        | Liveness and readiness probes            |
| GET    | /actuator/prometheus   | none        | Metrics scrape endpoint                  |

## Data

- PostgreSQL holds users and links; schema is managed by Flyway (`db/migration`).
- Redis caches code lookups, backs the fixed-window rate limiter, and carries the
  `clicks` stream.

## Run the tests

```bash
mvn verify
```

Integration tests use Testcontainers, so a running Docker engine is required.
The Docker Engine API version the test client negotiates is pinned in `pom.xml`
for compatibility with current engines.

## Build the image

```bash
docker build -t shortener-api:local .
```

Multi-stage build: Maven layer produces the jar, the runtime layer is a JRE base
running as a non-root user with the Spring Boot layered launcher.

## Configuration

All settings come from environment variables; see `.env.example`. Defaults in
`application.yml` target local development only.
