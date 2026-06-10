"""Tests for the analytics store, the stream consumer, and the HTTP API."""

from __future__ import annotations

from datetime import datetime, timezone

import pytest
from fakeredis import aioredis as fake_aioredis
from fastapi.testclient import TestClient

from app.config import Settings
from app.consumer import drain_once, ensure_group
from app.store import AnalyticsStore


def test_store_aggregates_clicks(tmp_path):
    store = AnalyticsStore(str(tmp_path / "a.duckdb"))
    ts = datetime(2026, 6, 10, 12, 0, tzinfo=timezone.utc)
    store.insert_click("abc", ts, "https://ref.example", "curl/8")
    store.insert_click("abc", ts, "https://ref.example", "curl/8")
    store.insert_click("abc", ts, "", "")

    stats = store.link_stats("abc")
    assert stats.total_clicks == 3
    assert stats.top_referrers[0].name == "https://ref.example"
    assert stats.top_referrers[0].count == 2
    assert sum(b.count for b in stats.by_day) == 3
    store.close()


@pytest.mark.asyncio
async def test_consumer_persists_stream_entries(tmp_path):
    store = AnalyticsStore(str(tmp_path / "c.duckdb"))
    redis = fake_aioredis.FakeRedis(decode_responses=True)
    # block_ms=0 makes drain_once non-blocking, which keeps the test deterministic.
    settings = Settings(duckdb_path=str(tmp_path / "c.duckdb"), block_ms=0)

    await ensure_group(redis, settings)
    await redis.xadd(
        settings.click_stream,
        {"code": "xyz", "ts": "2026-06-10T09:00:00Z", "referer": "https://a", "user_agent": "ua1"},
    )
    await redis.xadd(
        settings.click_stream,
        {"code": "xyz", "ts": "2026-06-10T10:00:00Z", "referer": "", "user_agent": "ua2"},
    )

    processed = await drain_once(redis, store, settings)

    assert processed == 2
    assert store.link_stats("xyz").total_clicks == 2
    await redis.aclose()
    store.close()


def test_api_returns_stats(tmp_path, monkeypatch):
    monkeypatch.setenv("ANALYTICS_DUCKDB_PATH", str(tmp_path / "api.duckdb"))
    monkeypatch.setattr(
        "app.main.aioredis.from_url",
        lambda *a, **k: fake_aioredis.FakeRedis(decode_responses=True),
    )

    # The API surface does not need the live consumer; park it so the event loop
    # stays free to serve requests during the test.
    async def _parked(redis, store, settings, stop):
        await stop.wait()

    monkeypatch.setattr("app.main.run_consumer", _parked)
    from app.main import app

    with TestClient(app) as client:
        store: AnalyticsStore = app.state.store
        store.insert_click(
            "demo", datetime(2026, 6, 10, 8, 0, tzinfo=timezone.utc), "https://x", "ua"
        )

        assert client.get("/healthz").json() == {"status": "ok"}
        body = client.get("/analytics/links/demo").json()
        assert body["code"] == "demo"
        assert body["total_clicks"] == 1

        metrics = client.get("/metrics")
        assert metrics.status_code == 200
