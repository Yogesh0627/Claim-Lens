# ClaimLens OCR Service

A small, stateless FastAPI service that extracts text and structured motor-insurance fields from claim
documents (images and PDFs). The backend claims OCR jobs and calls this service over HTTP; the service
owns **no database and no queue** — that keeps all tenant/transaction/job logic in the backend.

## Engines (swappable)

Selected by `OCR_ENGINE`, behind one `OcrEngine` interface:

- **`tesseract`** (default) — free, self-hosted, offline. The local-dev and free-host path. Needs the
  `tesseract` binary installed.
- **`vision`** — Google Cloud Vision. Higher accuracy for the deployed demo (generous free tier). Needs
  `requirements-vision.txt` + `GOOGLE_APPLICATION_CREDENTIALS`. Note: image data leaves your infra.

## API

- `GET /health` → `{ status, engine, engine_available, detail }`
- `POST /ocr` (multipart: `file`, optional `document_type`) →
  `{ engine, document_type, page_count, text, confidence, fields, warnings }`
  where `fields` holds best-effort `registration_numbers`, `policy_numbers`, `chassis_numbers`,
  `engine_numbers`, `dates`, `amounts`. These are **signals**, not ground truth (OCR is noisy).

If `OCR_SHARED_SECRET` is set, callers must send `X-OCR-Token: <value>`.

## Run locally

```bash
cd ocr-service
python -m venv .venv && . .venv/Scripts/activate    # Windows Git Bash;  or: source .venv/bin/activate
pip install -r requirements.txt
# Install Tesseract:  Windows -> https://github.com/UB-Mannheim/tesseract/wiki ;  Linux -> apt-get install tesseract-ocr
cp .env.example .env
uvicorn app.main:app --reload --port 8000
```

`GET http://localhost:8000/health` should report `engine_available: true` once Tesseract is installed.

## Docker

```bash
docker build -t claimlens-ocr .
docker run -p 8000:8000 claimlens-ocr        # Tesseract is baked into the image
```

## Test

```bash
pip install pytest
pytest                 # extraction tests always run; the OCR round-trip skips if Tesseract is absent
```
