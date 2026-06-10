"""FastAPI application exposing link analytics and a Prometheus endpoint."""

from __future__ import annotations

import asyncio
import logging
from contextlib import asynccontextmanager, suppress

from fastapi import FastAPI, Request, Response
from prometheus_fastapi_instrumentator import Instrumentator
from redis import asyncio as aioredis

from app.config import load_settings
from app.consumer import run_consumer
from app.logging_config import configure_logging
from app.models import LinkStats
from app.store import AnalyticsStore

log = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    configure_logging()
    settings = load_settings()
    store = AnalyticsStore(settings.duckdb_path)
    redis = aioredis.from_url(settings.redis_url, decode_responses=True)
    stop = asyncio.Event()
    log.info("Starting consumer task")
    task = asyncio.create_task(run_consumer(redis, store, settings, stop))

    def _on_done(t: asyncio.Task) -> None:
        if not t.cancelled() and t.exception() is not None:
            log.error("Consumer task exited with error", exc_info=t.exception())

    task.add_done_callback(_on_done)

    app.state.store = store
    app.state.redis = redis
    try:
        yield
    finally:
        stop.set()
        task.cancel()
        with suppress(asyncio.CancelledError):
            await task
        await redis.aclose()
        store.close()


app = FastAPI(title="analytics-svc", lifespan=lifespan)


@app.get("/healthz")
async def healthz() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/readyz")
async def readyz(request: Request, response: Response) -> dict[str, str]:
    store: AnalyticsStore = request.app.state.store
    redis: aioredis.Redis = request.app.state.redis
    try:
        await redis.ping()
        await asyncio.to_thread(store.ping)
    except Exception:
        response.status_code = 503
        return {"status": "unavailable"}
    return {"status": "ready"}


@app.get("/analytics/links/{code}", response_model=LinkStats)
async def link_stats(code: str, request: Request) -> LinkStats:
    store: AnalyticsStore = request.app.state.store
    return await asyncio.to_thread(store.link_stats, code)


Instrumentator().instrument(app).expose(app)
