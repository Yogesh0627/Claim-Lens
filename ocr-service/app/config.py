"""Runtime configuration, sourced from environment variables (see .env.example)."""

from __future__ import annotations

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    ocr_engine: str = "tesseract"
    tesseract_lang: str = "eng"
    tesseract_cmd: str | None = None
    max_pdf_pages: int = 15
    pdf_render_dpi: int = 200
    google_application_credentials: str | None = None
    ocr_shared_secret: str | None = None

    # Guard rails for uploads.
    max_upload_mb: int = 25


settings = Settings()
