"""Redis stream consumer that loads click events into the analytics store."""

from __future__ import annotations

import asyncio
import logging
from datetime import datetime

from redis import asyncio as aioredis
from redis.exceptions import ResponseError

from app.config import Settings
from app.store import AnalyticsStore

log = logging.getLogger(__name__)


def _parse_timestamp(raw: str) -> datetime:
    try:
        return datetime.fromisoformat(raw.replace("Z", "+00:00"))
    except (ValueError, AttributeError):
        return datetime.now()


async def ensure_group(redis: aioredis.Redis, settings: Settings) -> None:
    try:
        await redis.xgroup_create(
            name=settings.click_stream, groupname=settings.consumer_group, id="0", mkstream=True
        )
    except ResponseError as exc:
        if "BUSYGROUP" not in str(exc):
            raise


async def drain_once(redis: aioredis.Redis, store: AnalyticsStore, settings: Settings) -> int:
    """Read one batch from the stream, persist it, and acknowledge. Returns the
    number of entries processed. ``block_ms`` of 0 or None reads without blocking."""
    response = await redis.xreadgroup(
        groupname=settings.consumer_group,
        consumername=settings.consumer_name,
        streams={settings.click_stream: ">"},
        count=settings.read_count,
        block=settings.block_ms or None,
    )
    if not response:
        return 0

    processed = 0
    for _stream, entries in response:
        acked: list[str] = []
        for entry_id, fields in entries:
            try:
                await asyncio.to_thread(
                    store.insert_click,
                    fields.get("code", ""),
                    _parse_timestamp(fields.get("ts", "")),
                    fields.get("referer", ""),
                    fields.get("user_agent", ""),
                )
            except Exception:
                log.exception("Failed to persist entry %s", entry_id)
            acked.append(entry_id)
            processed += 1
        if acked:
            await redis.xack(settings.click_stream, settings.consumer_group, *acked)
    return processed


async def run_consumer(
    redis: aioredis.Redis, store: AnalyticsStore, settings: Settings, stop: asyncio.Event
) -> None:
    """Read from the click stream and persist entries until ``stop`` is set.

    The consumer group is created lazily inside the loop and re-created after any
    failure, so a Redis that is not yet reachable at startup self-heals once it
    becomes available rather than killing the consumer permanently.
    """
    log.info("Consumer starting for stream '%s'", settings.click_stream)
    group_ready = False
    while not stop.is_set():
        try:
            if not group_ready:
                await ensure_group(redis, settings)
                group_ready = True
                log.info("Consumer group ready")
            await drain_once(redis, store, settings)
        except asyncio.CancelledError:
            raise
        except Exception:
            log.exception("Consumer iteration failed; retrying")
            group_ready = False
            await asyncio.sleep(1)
    log.info("Consumer stopped")
