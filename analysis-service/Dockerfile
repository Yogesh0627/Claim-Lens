# ClaimLens image-analysis service. opencv-python-headless needs a couple of shared libs at runtime.
FROM python:3.12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends libglib2.0-0 libgl1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

# The host injects PORT; default to 8001 locally. Shell form so ${PORT} expands.
EXPOSE 8001
CMD uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8001}
