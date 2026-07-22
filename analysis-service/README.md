# ClaimLens Analysis Service

A small, stateless FastAPI service that produces **image fraud signals** (never decisions) for claim
photos. Lightweight CV only — no deep learning, no GPU. Like the OCR service, it owns no database: the
backend stores the returned hashes and does cross-claim duplicate comparison itself.

## Signals

- **Duplicate detection** — perceptual hashes (average / dHash / **pHash** via `imagehash`). The
  backend compares a new photo's pHash against other claims' hashes (Hamming distance) to catch reuse
  — the top motor-fraud pattern.
- **Similarity** — **ORB** feature matching (OpenCV) via `/compare`, catching lightly-edited reuse
  (crop / rotate / recolour) that hashing alone misses. ORB not SIFT: faster, free.
- **EXIF — three-state** `CONSISTENT / INCONSISTENT / UNKNOWN`. Only **INCONSISTENT** (capture time
  before the incident, GPS far from the reported location) is a fraud signal. **Missing EXIF is
  UNKNOWN, never suspicious** — social apps strip it, so absence is the norm.
- **Synthetic / tampered** — a **soft signal only** (no reliable detector exists): ELA recompression
  heuristic + C2PA provenance-manifest presence. Weighted low; never an auto-reject. A proper
  CNN/commercial detector is the V2 swap behind the same function.

## API

- `GET /health` → `{ status, checks }`
- `POST /analyze` (multipart: `file`, optional `incident_date`, `expected_lat`, `expected_lon`) →
  `{ width, height, hashes, exif, synthetic, warnings }`
- `POST /compare` (multipart: `file_a`, `file_b`) →
  `{ orb_similarity, orb_matches, phash_hamming, duplicate_candidate }`

If `ANALYSIS_SHARED_SECRET` is set, callers must send `X-ANALYSIS-TOKEN: <value>`.

## Run locally

```bash
cd analysis-service
python -m venv .venv && . .venv/Scripts/activate     # or: source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --port 8001
```

## Docker

```bash
docker build -t claimlens-analysis .
docker run -p 8001:8001 claimlens-analysis
```

## Test

```bash
pip install -r requirements-dev.txt
pytest        # all tests run — no external binaries needed
```
