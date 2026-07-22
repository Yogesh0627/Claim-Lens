"""Runtime configuration, sourced from environment variables (see .env.example)."""

from __future__ import annotations

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    max_upload_mb: int = 25
    duplicate_hamming_threshold: int = 8
    orb_similarity_threshold: float = 0.25
    exif_skew_hours: int = 24
    exif_gps_km_threshold: float = 50.0
    analysis_shared_secret: str | None = None


settings = Settings()
