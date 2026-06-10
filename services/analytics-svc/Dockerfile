# syntax=docker/dockerfile:1

# --- build stage ---------------------------------------------------------
FROM python:3.12-slim AS build
WORKDIR /app
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
COPY pyproject.toml .
COPY app ./app
RUN pip install --no-cache-dir --upgrade pip && pip install --no-cache-dir .

# --- runtime stage -------------------------------------------------------
FROM python:3.12-slim AS runtime
WORKDIR /app
# Patch OS packages to clear fixable base-image advisories, then add a non-root user.
RUN apt-get update \
    && apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --home-dir /app app \
    && mkdir -p /data && chown app:app /data
COPY --from=build /opt/venv /opt/venv
COPY app ./app
ENV PATH="/opt/venv/bin:$PATH" \
    ANALYTICS_DUCKDB_PATH=/data/analytics.duckdb
USER app
EXPOSE 8000
ENTRYPOINT ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
