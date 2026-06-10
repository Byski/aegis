# Aegis

An observable, self-healing, cloud-native service platform on Kubernetes.

Aegis is a small multi-service backend operated like a production system. The
application itself (a URL shortener with a click-analytics path) is intentionally
plain. The point of the project is the operability layer around it: SLOs and error
budgets, metrics and traces and structured logs, alerting, infrastructure as code,
automated CI/CD with security gating, and an automated recovery path under failure.

## Architecture

```
client -> shortener-api (Java 21 / Spring Boot)  -- redirect + JWT/RBAC admin
              |  cache + rate limit (Redis)
              |  click events (Redis Stream)
              v
          analytics-svc (Python / FastAPI)        -- consumes stream into DuckDB
              |
   Postgres (links, users)   Redis (cache, stream)   DuckDB (analytics store)

   metrics  -> Prometheus -> Grafana (RED/USE + SLO dashboards)
   traces   -> OpenTelemetry Collector -> Tempo
   alerts   -> Alertmanager (multi-window burn-rate rules)
```

## Stack

- Backend: Java 21 + Spring Boot (REST, validation, JWT auth and RBAC).
- Sidecar: Python + FastAPI analytics service over DuckDB.
- Data: PostgreSQL, Redis, DuckDB.
- Orchestration: Docker multi-stage builds, Kubernetes on a local kind cluster, Helm.
- Observability: Prometheus, Grafana, OpenTelemetry, Tempo, Alertmanager.
- IaC: Terraform, validated locally against LocalStack.
- CI/CD: GitHub Actions with Testcontainers integration tests and Trivy image scans.
- Load testing: k6.

## Getting started

```bash
make bootstrap   # install toolchain, wire git hooks (run once)
make help        # list available targets
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for code standards and workflow.

## Status

Under active construction. See [docs/roadmap.md](docs/roadmap.md) for remaining
work and resume notes. Build phases:

- [x] Phase 0: scaffold and hygiene gate
- [x] Phase 1: shortener-api with CI
- [x] Phase 2: analytics-svc and local cluster
- [ ] Phase 3: observability and SLOs (metrics, SLOs, alerting, dashboard done; tracing pending)
- [ ] Phase 4: infrastructure as code, v1
- [ ] Phase 5: resilience (HPA, chaos, load)
- [ ] Phase 6: polish
