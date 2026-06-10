"""Response models for the analytics API."""

from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel


class TimeBucket(BaseModel):
    timestamp: datetime
    count: int


class NameCount(BaseModel):
    name: str
    count: int


class LinkStats(BaseModel):
    code: str
    total_clicks: int
    by_day: list[TimeBucket]
    top_referrers: list[NameCount]
    top_user_agents: list[NameCount]
