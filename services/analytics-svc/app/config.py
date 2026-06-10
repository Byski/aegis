"""Application settings, sourced from environment variables."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="ANALYTICS_", env_file=".env")

    redis_url: str = "redis://localhost:6379/0"
    click_stream: str = "clicks"
    consumer_group: str = "analytics"
    consumer_name: str = "analytics-1"
    # DuckDB file location. Use a writable path backed by a volume in the cluster.
    duckdb_path: str = "analytics.duckdb"
    # How many stream entries to claim per read, and how long to block (ms).
    read_count: int = 50
    block_ms: int = 2000


def load_settings() -> Settings:
    return Settings()
