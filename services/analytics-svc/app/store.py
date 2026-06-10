"""DuckDB-backed analytics store.

DuckDB connections are not safe for concurrent use, so every operation is
guarded by a single lock. Callers in async contexts should invoke these methods
through ``asyncio.to_thread`` to avoid blocking the event loop.
"""

from __future__ import annotations

import threading
from datetime import datetime

import duckdb

from app.models import LinkStats, NameCount, TimeBucket

_SCHEMA = """
create table if not exists clicks (
    code        varchar     not null,
    ts          timestamp   not null,
    referer     varchar,
    user_agent  varchar,
    ingested_at timestamp   not null default now()
);
create index if not exists idx_clicks_code on clicks (code);
"""


class AnalyticsStore:
    def __init__(self, path: str) -> None:
        self._lock = threading.Lock()
        self._conn = duckdb.connect(path)
        with self._lock:
            self._conn.execute(_SCHEMA)

    def insert_click(self, code: str, ts: datetime, referer: str, user_agent: str) -> None:
        with self._lock:
            self._conn.execute(
                "insert into clicks (code, ts, referer, user_agent) values (?, ?, ?, ?)",
                [code, ts, referer or None, user_agent or None],
            )

    def ping(self) -> bool:
        with self._lock:
            self._conn.execute("select 1")
        return True

    def link_stats(self, code: str) -> LinkStats:
        with self._lock:
            total = self._conn.execute(
                "select count(*) from clicks where code = ?", [code]
            ).fetchone()[0]

            by_day_rows = self._conn.execute(
                """
                select date_trunc('day', ts) as bucket, count(*) as n
                from clicks where code = ?
                group by bucket order by bucket
                """,
                [code],
            ).fetchall()

            referer_rows = self._conn.execute(
                """
                select coalesce(referer, '(direct)') as name, count(*) as n
                from clicks where code = ?
                group by name order by n desc limit 10
                """,
                [code],
            ).fetchall()

            ua_rows = self._conn.execute(
                """
                select coalesce(user_agent, '(unknown)') as name, count(*) as n
                from clicks where code = ?
                group by name order by n desc limit 10
                """,
                [code],
            ).fetchall()

        return LinkStats(
            code=code,
            total_clicks=int(total),
            by_day=[TimeBucket(timestamp=row[0], count=int(row[1])) for row in by_day_rows],
            top_referrers=[NameCount(name=row[0], count=int(row[1])) for row in referer_rows],
            top_user_agents=[NameCount(name=row[0], count=int(row[1])) for row in ua_rows],
        )

    def close(self) -> None:
        with self._lock:
            self._conn.close()
