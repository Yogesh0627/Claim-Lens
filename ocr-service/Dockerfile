# ClaimLens OCR service. Ships with Tesseract installed so the default engine works out of the box.
FROM python:3.12-slim

# Tesseract + language data. Add e.g. tesseract-ocr-hin for Hindi.
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# For the Vision engine, also: RUN pip install --no-cache-dir -r requirements-vision.txt
COPY requirements-vision.txt .

COPY app ./app

# The host injects PORT; default to 8000 locally. Shell form so ${PORT} expands.
EXPOSE 8000
ENV OCR_ENGINE=tesseract
CMD uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}
