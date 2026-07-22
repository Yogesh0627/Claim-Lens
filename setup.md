# 🛠️ Local Setup Guide

Get **ClaimLens** running on your machine, step by step. Total time: ~15 minutes.

> **TL;DR**
> 1. Start Postgres → create two databases
> 2. Backend: copy `.env`, run `./mvnw spring-boot:run` → http://localhost:8080
> 3. Frontend: `npm install && npm run dev` → http://localhost:3000
> 4. (Optional) Analysis service for image forensics → http://localhost:8001
> 5. Log in with a one-click demo account 🎉

---

## 1. Prerequisites

| Tool | Version | Why |
|------|---------|-----|
| ☕ **JDK** | 21 | Backend (Spring Boot 4) |
| 🐘 **PostgreSQL** | 17 | Database (Flyway migrates the schema for you) |
| 🟢 **Node.js** | 20+ | Frontend (Next.js 16) |
| 🐍 **Python** | 3.12 | *Optional* — image-analysis service |
| 🔧 **Git** | any | Clone the repo |

> The backend uses the Maven wrapper (`./mvnw`), so you **don't** need Maven installed.
> On Windows, run the shell commands below in **Git Bash** (they use `source`).

---

## 2. Database 🐘

Create the two databases the app uses (a dev DB and a test DB):

```bash
# using psql (adjust the path/user for your install)
psql -U postgres -c "CREATE DATABASE claimlens_db;"
psql -U postgres -c "CREATE DATABASE claimlens_test;"
```

**Prefer Docker?** A compose file ships Postgres + Redis:

```bash
docker compose up -d          # starts postgres:17 on :5432 and redis:7 on :6379
```

> You do **not** create tables by hand — **Flyway** runs all 26 migrations automatically on the first backend start. ✨

---

## 3. Backend ⚙️ (http://localhost:8080)

```bash
cd backend

# 1. Create your local env file from the template
cp .env.example .env
```

Open `backend/.env` and set at minimum:

```bash
DATABASE_PASSWORD=your_postgres_password
JWT_SECRET=any-random-string-at-least-32-bytes-long-0000
DEMO_SEED=true          # seeds a demo tenant + one login per role
```

Everything else has safe defaults (OCR/AI/email are off unless you add keys — see [§7](#7-optional-integrations-)).

**Run it** — the app loads `backend/.env` automatically (via `spring.config.import`), so there's nothing to source:

```bash
./mvnw spring-boot:run
```

> 💡 This also means the **IDE Run button works out of the box** — IntelliJ/VS Code don't need any env-var setup; the app reads `.env` itself. On deploy (Render), there's no `.env` file and the platform injects real env vars — the import is `optional:`, so it boots fine either way.

On first boot you'll see Flyway apply the migrations and the demo data seed. The API is now live at **http://localhost:8080** 🚀

✅ **Verify:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.claimlens.app","password":"Password123!"}'
# → 200 with an accessToken
```

---

## 4. Frontend 🎨 (http://localhost:3000)

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:3000**. The frontend talks to the backend at `http://localhost:8080/api/v1` by default — no config needed for local dev.

> To point it elsewhere, set `NEXT_PUBLIC_API_BASE_URL` in `frontend/.env.local`.

---

## 5. Log in 🔑

On the login page, click any **one-click demo account** (or type the email + password `Password123!`):

| Role | Email | Lands on |
|------|-------|----------|
| 🏢 Platform Admin | `platformadmin@demo.claimlens.app` | Cross-tenant console |
| 👔 Tenant Admin | `admin@demo.claimlens.app` | Full workspace |
| 🕵️ Investigation Manager | `manager@demo.claimlens.app` | Assigns claims |
| 🔍 Investigator | `investigator@demo.claimlens.app` | Decides claims |
| 🙋 Customer (Policyholder) | `chauhanyogesh950+rahul@gmail.com` | Self-service portal |
| 📞 Customer Support | `support@demo.claimlens.app` | Files claims |
| 🧑‍💼 Employee (Adjuster) | `employee@demo.claimlens.app` | Policy/coverage view |
| 📊 Auditor | `auditor@demo.claimlens.app` | Analytics & audit |

All demo accounts use the password **`Password123!`** (sandbox only).

---

## 6. Optional — Analysis service 🖼️ (image forensics)

Enables duplicate-photo detection, ORB similarity, EXIF checks and synthetic-image signals.

```bash
cd analysis-service
python -m venv .venv
source .venv/Scripts/activate     # Windows Git Bash
# source .venv/bin/activate       # macOS / Linux
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001
```

Then in `backend/.env` set `ANALYSIS_ENABLED=true` and restart the backend. Check it:
```bash
curl http://localhost:8001/health
# → {"status":"UP","opencv":true,"imageHash":true,"orb":true,"syntheticModel":true,"version":"1.0.0"}
```

> Without this service the app runs fine — image analysis is simply skipped (fraud still scores on rules + OCR).

---

## 7. Optional — Integrations 🔌

All off by default; the app works without any of them. Add keys in `backend/.env` to switch each on:

| Feature | Env to set | Off-state behaviour |
|---------|-----------|---------------------|
| 🔎 **OCR (Google Vision)** | `OCR_ENABLED=true`, `OCR_PROVIDER=vision`, `GOOGLE_VISION_API_KEY=...` | No text extracted |
| 🤖 **AI answers (Gemini RAG)** | `AI_ENABLED=true`, `GEMINI_API_KEY=...` | Offline stub (extractive answers) |
| 📧 **Email (Resend)** | `EMAIL_PROVIDER=resend`, `RESEND_API_KEY=...`, `EMAIL_FROM=...` | Emails logged, not sent |
| 🗄️ **Object storage (R2/S3)** | `STORAGE_PROVIDER=s3` + `STORAGE_S3_*` | Local filesystem |
| ⚡ **Redis cache** | `SPRING_PROFILES_ACTIVE=prod` + `REDIS_*` | In-process (Caffeine) — no setup needed locally |
| 🔐 **Google login** | `GOOGLE_CLIENT_ID=...` | Email/password only |

> ⚡ **Caching:** local dev uses an in-process cache (Caffeine) automatically — you don't need Redis running. The `docker compose` Redis is only there if you want to exercise the production (`prod`) profile locally.

---

## 8. Running the tests 🧪

```bash
cd backend
./mvnw test          # reads backend/.env automatically; no env vars to pass
# → 74 tests, 2 skipped (a live-Vision test and a manual email-sender — both run only when their env vars are set)
```

Tests run against the **`claimlens_test`** database and never touch your dev data.

```bash
cd analysis-service && ./.venv/Scripts/python.exe -m pytest -q   # image-service tests
cd frontend && npx tsc --noEmit && npm run build                 # frontend typecheck + build
```

---

## 9. Troubleshooting 🩺

| Symptom | Fix |
|---------|-----|
| Backend won't start, DB auth error | `DATABASE_PASSWORD` in `.env` doesn't match Postgres |
| `JWT_SECRET` error on boot | It has no default on purpose — set one (≥ 32 chars) |
| Frontend CORS error | Make sure the backend is on `:8080` and the frontend on `:3000` (default `CORS_ALLOWED_ORIGINS`) |
| Demo login fails | Set `DEMO_SEED=true` and restart the backend (it seeds on boot, idempotently) |
| Port already in use | Something else is on `:8080` / `:3000` / `:8001` — stop it or change the port |

---

Ready to ship it to the cloud? See **[deploy.md](deploy.md)** ☁️
