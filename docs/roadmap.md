# Roadmap and resume notes

This document is the single source of truth for picking the project back up. It
records what is done, what remains, how to run things locally, and the
environment gotchas that cost time the first time around.

## Current state (done and merged to main)

- **Phase 0 - scaffold and hygiene gate.** Repo skeleton, `Makefile`,
  `scripts/bootstrap.sh`, and an automated hygiene gate
  (`scripts/check-hygiene.sh`) wired as a pre-commit hook and a CI workflow.
- **Phase 1 - shortener-api.** Java 21 / Spring Boot service: JWT auth with
  USER and ADMIN roles, Postgres via Flyway, Redis cache and fixed-window rate
  limiting, a public 302 redirect that publishes click events to a Redis stream,
  Actuator metrics and probes, Testcontainers integration tests, multi-stage
  image, and the `ci-java` workflow.
- **Phase 2 - analytics-svc and local cluster.** Python / FastAPI service that
  consumes the click stream into DuckDB and serves per-link analytics. Resilient
  consumer (re-establishes its group if Redis is briefly unavailable). pytest
  suite with fakeredis. Self-contained Helm chart (Postgres, Redis, both
  services), kind cluster config, and the `ci-python` workflow.
- **Phase 3a - metrics, SLOs, alerting, dashboard.** ServiceMonitors scrape both
  services through kube-prometheus-stack. A PrometheusRule defines redirect SLIs,
  a 99.9% availability objective with multiwindow multi-burn-rate alerts, and a
  p99 latency alert. A provisioned Grafana dashboard shows RED panels plus error
  budget and burn rate. See `docs/observability.md`.

## Remaining work

### Phase 3b - distributed tracing
- Add OpenTelemetry to `shortener-api`: `micrometer-tracing-bridge-otel` and
  `opentelemetry-exporter-otlp`; configure `management.otlp.tracing.endpoint` and
  a sampling probability.
- Add OpenTelemetry to `analytics-svc`: `opentelemetry-instrumentation-fastapi`
  and the OTLP exporter; set `OTEL_EXPORTER_OTLP_ENDPOINT`.
- Deploy an OpenTelemetry Collector and Tempo (values already stubbed in
  `deploy/helm/observability/values.yaml`). Add a Grafana Tempo datasource as a
  labelled ConfigMap.
- Rebuild both images, re-scan with Trivy, redeploy, and verify a trace spans
  both services in Grafana.

### Phase 4 - infrastructure as code and v1
- Terraform under `terraform/`: modules for managed Postgres, a container
  registry, and DNS. Validate with `terraform validate` and `plan` against
  LocalStack (via `tflocal`); never apply to a real cloud account.
- Add a `deploy-smoke` GitHub Actions job that spins kind inside the runner,
  installs the chart, and runs a smoke test.
- Write the top-level README with an architecture diagram, an SLO table, a
  `make demo` one-command path, and the hard numbers placeholders.
- This is the shippable v1.

### Phase 5 - resilience
- HorizontalPodAutoscaler for `shortener-api` (CPU first; optionally a custom
  request-rate metric via prometheus-adapter).
- Chaos experiments: kill a pod and inject latency; a script that measures
  recovery time.
- A k6 load test that drives the HPA and the dashboard.
- Alertmanager routing and receivers for the burn-rate alerts.
- Capture the numbers: MTTR for the injected failure, sustained requests per
  second, p99 latency.

### Phase 6 - polish
- Short walkthrough recording, README hard numbers (p99, requests per second,
  MTTR, test coverage, image size before and after), a runbook, and a `v1.0` tag.

## Running locally

```bash
make bootstrap       # one-time toolchain install
make up              # kind cluster + both services + datastores
make observability   # kube-prometheus-stack + SLO rules + dashboard
make down            # tear down the cluster
```

Service endpoints when up: shortener at `http://localhost:30080`, analytics at
`http://localhost:30081`. Grafana: `kubectl -n monitoring port-forward
svc/kube-prometheus-stack-grafana 3000:80` (admin / prom-operator).

End-to-end smoke: register, login, create a link, redirect to it a few times,
then read `GET /analytics/links/{code}` and confirm the click count rises.

## Environment notes that saved time

- **Java 21** lives at `/opt/homebrew/opt/openjdk@21`. Build locally with that as
  `JAVA_HOME`. CI uses Temurin 21.
- **Testcontainers and Docker Engine.** Modern Docker Engine raised its minimum
  API version, so the Testcontainers client is pinned to `api.version=1.44` in
  `services/shortener-api/pom.xml`. This value works locally and on CI runners.
- **Image scanning.** The Trivy gate fails on fixable HIGH and CRITICAL findings.
  The working approach: bump framework and transitive dependency versions to the
  patched releases, and run an OS package upgrade in slim base images. Re-scan
  locally with the Trivy container before pushing to avoid red CI.
- **Redirect metric label.** The Micrometer `uri` tag for the redirect is the
  full route form `/{code:[A-Za-z0-9_-]+}`. SLO queries must use that exact value.
- **Empty-vector ratios.** With zero 5xx samples, a bare `sum(rate(...5xx...))`
  yields no data rather than zero, which breaks ratio rules. The error-ratio
  rules wrap the numerator with `or vector(0)`.
- **Analytics consumer.** It creates its consumer group lazily inside the loop
  and retries, so a not-yet-ready Redis at startup self-heals.
- **kind specifics.** Cluster name `aegis`; NodePorts 30080 and 30081 are mapped
  to the host in `deploy/kind/cluster.yaml`. After building images, load them
  with `kind load docker-image ... --name aegis` (handled by `make up`).

## Workflow conventions

- One feature branch per phase off `main`, conventional commit messages, and a
  pull request per phase merged after CI is green.
- The hygiene gate forbids vendor or assistant references, em-dashes, emoji, and
  committed secrets. Keep all committed artifacts professional and neutral.
