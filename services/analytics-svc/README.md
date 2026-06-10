# analytics-svc

Python + FastAPI service. Consumes click events from the Redis `clicks` stream
(produced by `shortener-api`) into DuckDB and serves per-link analytics.

## Endpoints

| Method | Path                      | Purpose                                   |
|--------|---------------------------|-------------------------------------------|
| GET    | /analytics/links/{code}   | Totals, daily series, top referrers and UAs |
| GET    | /healthz                  | Liveness probe                            |
| GET    | /readyz                   | Readiness probe (checks Redis and DuckDB) |
| GET    | /metrics                  | Prometheus metrics                        |

## How it works

A background consumer reads the stream with a consumer group, writes each event
into a DuckDB table, and acknowledges it. The consumer logic is split into a
non-blocking `drain_once` step (used by tests) and a `run_consumer` loop (used at
runtime), so the ingestion path is deterministic under test.

## Run the tests

```bash
python3.12 -m venv .venv && .venv/bin/pip install -e ".[dev]"
.venv/bin/ruff check app tests
.venv/bin/pytest
```

Tests use `fakeredis` and a temporary DuckDB file, so no external services are
required.

## Build the image

```bash
docker build -t analytics-svc:local .
```

Multi-stage build on a slim Python base, running as a non-root user. DuckDB is
written to `/data` (mount a volume in the cluster).

## Configuration

All settings come from `ANALYTICS_`-prefixed environment variables; see
`.env.example`.
