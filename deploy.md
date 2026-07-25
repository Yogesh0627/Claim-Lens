# ☁️ Deployment Guide

Deploy **ClaimLens** to a free, production-style cloud stack, step by step.

## 🧭 The target architecture

```
                    ┌──────────────────────┐
   Browser ───────► │  Frontend (Vercel)   │  Next.js
                    └──────────┬───────────┘
                               │ HTTPS  (NEXT_PUBLIC_API_BASE_URL)
                    ┌──────────▼───────────┐
                    │  Backend (Render)    │  Spring Boot (Docker)
                    │  OCR = Google Vision │
                    └───┬───────┬───────┬──┘
          ┌─────────────┘       │       └──────────────┐
   ┌──────▼──────┐   ┌──────────▼─────────┐   ┌─────────▼────────┐
   │ Neon (DB)   │   │ analysis-service   │   │  R2 (storage)    │
   │ Postgres    │   │ (Render, Docker)   │   │  Resend (email)  │
   └─────────────┘   └────────────────────┘   └──────────────────┘

   ⏰ cron-job.org  ── pings the backend `GET /` every 10 min, 10-hour window (keep-alive)
```

| Piece | Host | Free tier |
|-------|------|-----------|
| 🐘 Database | **Neon** | ✅ |
| ⚙️ Backend (Java) | **Render** (Docker web service) | ✅ (sleeps after 15 min idle) |
| 🖼️ Analysis service (Python) | **Render** (Docker web service) | ✅ |
| 🎨 Frontend (Next.js) | **Vercel** | ✅ |
| 🗄️ Object storage | **Cloudflare R2** | ✅ |
| ⚡ Cache | **Upstash Redis** | ✅ (free tier; one account can be shared across apps) |
| 📧 Email | **Resend** | ✅ |
| 🔎 OCR | **Google Cloud Vision** | Free tier (billing must be enabled) |
| ⏰ Keep-alive | **cron-job.org** | ✅ |

> 💡 **Cost:** $0/month. The main caveat is Render free services **sleep after ~15 min idle** — the cron pinger in [Step 8](#step-8--keep-alive-cron-cron-jobrg-) keeps the backend warm during active hours. Render's 750 instance-hours are **pooled across the whole account**, so Step 8 uses a 10-hour daily window rather than 24/7.

---

## ✅ Prerequisites (accounts to create)

- [ ] **GitHub** account (Render + Vercel deploy *from* GitHub)
- [ ] **Neon** account → a Postgres project
- [ ] **Render** account
- [ ] **Vercel** account
- [ ] **Cloudflare R2** bucket + API token
- [ ] **Upstash** account → a Redis database (free tier; you can reuse one you already have — keys are namespaced)
- [ ] **Resend** account + a verified sending domain
- [ ] **Google Cloud** project with the **Vision API enabled and billing on**
- [ ] **cron-job.org** account

---

## Step 1 — Push the code to GitHub 🐙

Render and Vercel deploy from a git host, so the code must live in **your** GitHub repo.

```bash
cd Claim-Lens
git init
git add .
git commit -m "Initial commit — ClaimLens"
git branch -M main
git remote add origin https://github.com/<you>/claim-lens.git
git push -u origin main
```

> 🔒 The `.env` files are gitignored — your secrets are **not** pushed. Only `.env.example` templates are. You'll set the real secrets in each host's dashboard.

---

## Step 2 — Database on Neon 🐘

1. In Neon, create a project → you get a connection string like:
   ```
   postgresql://<user>:<password>@ep-xxxx.<region>.aws.neon.tech/neondb?sslmode=require
   ```
2. You need the **direct** endpoint (the host **without** `-pooler`) — the app runs Flyway on boot, and Flyway's advisory locks don't work through Neon's pooler.
3. Convert it to the JDBC form you'll paste into Render:
   ```
   jdbc:postgresql://ep-xxxx.<region>.aws.neon.tech/neondb?sslmode=require
   ```
4. That's it — **you don't run any SQL**. When the backend first boots on Render, Flyway creates the whole schema and (if `DEMO_SEED=true`) seeds the demo tenant. ✨

> **pgvector.** `PGVECTOR_ENABLED=true` is set for Render (Neon ships the extension); local dev stays
> `false` because stock Postgres doesn't have it. On boot the backend runs `CREATE EXTENSION vector`,
> adds `policy_chunk.embedding_vec` and builds the HNSW index — all idempotent.
>
> ⚠️ **It does not backfill.** Policy chunks ingested while pgvector was off have a NULL vector and
> are skipped by the search, so coverage answers come back empty. **Ingest policy documents after the
> first boot with this enabled**, or re-ingest them. Confirm it's live by looking for
> `pgvector enabled: extension + policy_chunk.embedding_vec + HNSW index ready` in the logs.

---

## Step 2b — Redis cache on Upstash ⚡

The backend caches permission resolution. Locally it's an in-process cache (nothing to set up); **in production it uses Redis**, activated by the `prod` profile.

1. In **Upstash**, create a **Redis** database (any region — pick one near your Render region).
2. From its details page, copy the **Endpoint** (host), **Port** (`6379`) and **Password**.
3. Keep them for Step 3's env vars (`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`).

> 💡 **Sharing one Upstash account across apps is fine.** ClaimLens namespaces every key with a `claimlens:` prefix, so it won't collide with another app's keys in the same database.

---

## Step 3 — Backend on Render ⚙️

1. **New → Web Service → Build from a Git repository** → pick your repo.
2. Settings:
   - **Root Directory:** `backend`
   - **Runtime / Environment:** `Docker` (Render auto-detects `backend/Dockerfile`)
   - **Instance type:** Free
3. Add the **environment variables** (⇩ full checklist below). The important ones:

```
DATABASE_URL       = jdbc:postgresql://ep-xxxx.<region>.aws.neon.tech/neondb?sslmode=require
DATABASE_USERNAME  = <neon user>
DATABASE_PASSWORD  = <neon password>
JWT_SECRET         = <random ≥32-byte string>
SPRING_PROFILES_ACTIVE = prod                            # switches the cache to Redis
REDIS_HOST         = <your-db>.upstash.io                # from Step 2b
REDIS_PORT         = 6379
REDIS_PASSWORD     = <upstash password>
CORS_ALLOWED_ORIGINS = https://<your-app>.vercel.app     # add after Step 5
APP_BASE_URL         = https://<your-app>.vercel.app     # add after Step 5 (email links)
DEMO_SEED          = true
```

> ⚠️ **If you skip Upstash:** leave `SPRING_PROFILES_ACTIVE` unset. The app then uses its in-process cache and boots fine — you just don't get a shared/persistent cache across restarts. Setting `SPRING_PROFILES_ACTIVE=prod` **without** the `REDIS_*` vars will fail to start (it expects a Redis host).

4. **Create Web Service.** Render builds the Docker image and starts it. First boot runs the migrations + seed (watch the logs).
5. Note the URL, e.g. `https://claimlens-backend.onrender.com`.

✅ **Verify:** `https://claimlens-backend.onrender.com/actuator/health` → `{"status":"UP"}`

> The Dockerfile already binds to Render's injected `$PORT` and tunes the JVM heap for the 512 MB free tier — nothing to configure.

---

## Step 4 — Analysis service on Render 🖼️ (optional but recommended)

For image-fraud forensics (duplicate photos, EXIF, synthetic detection).

1. **New → Web Service** → same repo.
2. Settings:
   - **Root Directory:** `analysis-service`
   - **Environment:** `Docker`
   - **Instance type:** Free
3. No required env vars (add `ANALYSIS_SHARED_SECRET` if you want to lock it down).
4. Deploy → note the URL, e.g. `https://claimlens-analysis.onrender.com`.
5. **Back in the backend service**, set:
   ```
   ANALYSIS_ENABLED     = true
   ANALYSIS_SERVICE_URL = https://claimlens-analysis.onrender.com
   ```
   and redeploy the backend.

✅ **Verify:** `https://claimlens-analysis.onrender.com/health` → `{"status":"UP", ...}`

> Skipping this? Leave `ANALYSIS_ENABLED=false` — the app runs fine, image analysis is just skipped.

---

## Step 5 — Frontend on Vercel 🎨

1. **Add New → Project** → import your GitHub repo.
2. Settings:
   - **Root Directory:** `frontend`
   - Framework preset: **Next.js** (auto-detected)
3. Add an environment variable:
   ```
   NEXT_PUBLIC_API_BASE_URL = https://claimlens-backend.onrender.com/api/v1
   ```
   *(optional)* `NEXT_PUBLIC_GOOGLE_CLIENT_ID`, `NEXT_PUBLIC_DEMO=true`
4. **Deploy.** You get `https://<your-app>.vercel.app`.

---

## Step 6 — Wire the two sides together 🔗

1. **CORS:** in the **Render backend**, set
   `CORS_ALLOWED_ORIGINS = https://<your-app>.vercel.app`.
2. **Email links:** set `APP_BASE_URL = https://<your-app>.vercel.app` on the same service, then
   redeploy. This is what invitation and password-reset emails build their "set your password" links
   from — leave it unset and every link sends the recipient to `localhost`.
3. **API URL:** confirm the Vercel `NEXT_PUBLIC_API_BASE_URL` points at the Render backend `/api/v1`.

✅ Open the Vercel URL and log in with a demo account — you should reach the workspace.

---

## Step 7 — Google login origin 🔐 (if using Google sign-in)

In **Google Cloud Console → Credentials → your OAuth client**, add to
**Authorized JavaScript origins**:
```
https://<your-app>.vercel.app
```

---

## Step 8 — Keep-alive cron (cron-job.org) ⏰

Render free services sleep after ~15 min idle. A cron ping keeps the backend warm so a visitor never
hits a cold start. Two rules shape the structure below — get them wrong and you trade one free-tier
overage for another:

- **Ping `GET /`, never `/actuator/health`.** `/` ([`HealthController`](backend/src/main/java/com/niyotechnologies/claimlens/common/controller/HealthController.java)) returns a flat string and touches nothing. `/actuator/health` runs the DB indicator — a query every 10 min keeps **Neon's** compute endpoint from auto-suspending and quietly eats Neon's free monthly compute-hour cap. Both are `permitAll`, so it's purely a cost choice.
- **Cron the backend only — not the analysis service.** Render's 750 free instance-hours are **pooled workspace-wide** (shared with any other services in the same account, e.g. EcoExpress). The backend's `AnalysisWarmup` already fires a wake ping at startup, and if analysis is cold a claim just loses its image-forensics signals (fraud still scores on rules + OCR — a designed-for degradation). Cronning analysis would spend ~365 h/mo to avoid an occasional 60 s delay.

### The job

1. Sign in to **[cron-job.org](https://cron-job.org)** → **Create cronjob**.
2. Configure:
   - **Title:** ClaimLens backend keep-alive
   - **URL:** `https://claimlens-backend-n1yp.onrender.com/` *(your real backend URL)*
   - **Schedule → Custom (expert mode):** `5,15,25,35,45,55 10-19 * * *` — every 10 min, only 10:00–19:59. *(Use the explicit minute list, not `5-55/10` — cron-job.org's validator rejects the range-with-step form.)*
   - **Timezone:** Asia/Kolkata
   - Save.

That's a **10-hour daily window**, not 24/7. Ping frequency doesn't affect hours consumed — only the
window length does; the container is up continuously across the window either way. Ten minutes is
chosen so one missed ping still leaves 5 min of margin before Render's 15-min idle timeout.

### If you also run EcoExpress in the same Render account

The 750 h are shared, so window both services and **offset them by 5 min** (readability, and avoids two
cold starts on the same second):

| Service | URL (cheapest DB-free endpoint) | Cron expression | Window |
|---|---|---|---|
| ClaimLens backend | `…-n1yp.onrender.com/` | `5,15,25,35,45,55 10-19 * * *` | 10:00–19:59 |
| EcoExpress | its DB-free public endpoint | `*/10 10-19 * * *` | 10:00–19:59 |
| ClaimLens analysis | — | **no cron** | on-demand only |

**Budget** (last ping 19:50 → spin-down ~20:05 ≈ 10.1 h/day):

```
EcoExpress          10.1 h × 30 ≈ 303 h
ClaimLens backend   10.1 h × 30 ≈ 303 h
ClaimLens analysis  on-demand   ≈  20 h
                                ------
                                ≈ 626 h of 750  (~83%, ~124 h headroom)
```

> 💡 **Expect the first ping of each day to show as failed.** cron-job.org's free tier caps request
> timeout at 30 s; a cold Render start takes ~50 s. That ping still *triggers* the wake — the one 10 min
> later succeeds. It's one isolated failure per day, never a consecutive streak, so cron-job.org won't
> auto-disable the job. Don't chase it. **Vercel doesn't sleep, so the frontend needs no cron.**

---

## 📋 Full backend env-var checklist (Render)

| Variable | Value | Notes |
|----------|-------|-------|
| `DATABASE_URL` | `jdbc:postgresql://…neon.tech/neondb?sslmode=require` | **direct** endpoint (no `-pooler`) |
| `DATABASE_USERNAME` | `<neon user>` | |
| `DATABASE_PASSWORD` | `<neon password>` | 🔒 secret |
| `JWT_SECRET` | `<random ≥32 bytes>` | 🔒 secret, no default |
| `SPRING_PROFILES_ACTIVE` | `prod` | switches the cache backend to Redis |
| `REDIS_HOST` | `<your-db>.upstash.io` | from Upstash (Step 2b) |
| `REDIS_PORT` | `6379` | |
| `REDIS_PASSWORD` | `<upstash password>` | 🔒 secret |
| `CORS_ALLOWED_ORIGINS` | `https://<app>.vercel.app` | your Vercel domain |
| `APP_BASE_URL` | `https://<app>.vercel.app` | frontend URL used in invitation / password-reset email links — unset means every link points at localhost |
| `DEMO_SEED` | `true` | seed demo tenant + logins |
| `STORAGE_PROVIDER` | `s3` | use R2 for uploads |
| `STORAGE_S3_ENDPOINT` | `https://<acct>.r2.cloudflarestorage.com` | |
| `STORAGE_S3_REGION` | `auto` | |
| `STORAGE_S3_BUCKET` | `<bucket>` | |
| `STORAGE_S3_ACCESS_KEY` / `STORAGE_S3_SECRET_KEY` | R2 keys | 🔒 secret |
| `EMAIL_PROVIDER` | `resend` | |
| `RESEND_API_KEY` | `re_…` | 🔒 secret |
| `EMAIL_FROM` | `Claimlens <noreply@your-domain>` | verified Resend domain |
| `OCR_ENABLED` | `true` | |
| `OCR_PROVIDER` | `vision` | in-JVM Google Vision |
| `GOOGLE_VISION_API_KEY` | `AIza…` | 🔒 secret, billing enabled |
| `AI_ENABLED` | `true` | Gemini RAG (else offline stub) |
| `GEMINI_API_KEY` | `…` | 🔒 secret |
| `PGVECTOR_ENABLED` | `true` | pgvector + HNSW on Neon (local stays `false` — no extension). Needs `AI_ENABLED=true` for 768-dim vectors |
| `ANALYSIS_ENABLED` | `true` | image forensics |
| `ANALYSIS_SERVICE_URL` | `https://claimlens-analysis.onrender.com` | |
| `GOOGLE_CLIENT_ID` | `…apps.googleusercontent.com` | Google login |

**Vercel (frontend):**

| Variable | Value |
|----------|-------|
| `NEXT_PUBLIC_API_BASE_URL` | `https://claimlens-backend.onrender.com/api/v1` |
| `NEXT_PUBLIC_GOOGLE_CLIENT_ID` *(opt)* | `…apps.googleusercontent.com` |
| `NEXT_PUBLIC_DEMO` *(opt)* | `true` |

---

## 🩺 Deployment troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| Backend build ok but 502 / health fails | The app must bind `$PORT` — the Dockerfile does this; make sure you didn't override `server.port` |
| Flyway error on boot | Using the **pooler** endpoint — switch `DATABASE_URL` to the **direct** endpoint |
| Frontend loads but every API call fails (CORS) | `CORS_ALLOWED_ORIGINS` on the backend must exactly match the Vercel URL (https, no trailing slash) |
| First request after idle is slow | Render cold start — the cron pinger (Step 8) prevents this during active hours |
| Deploy fails **"timed out waiting for the service"**, but the logs show the app *did* start | The health check was failing, so Render killed a running service. Redis health no longer gates `/actuator/health`, and cache errors can no longer break requests — redeploy on current code. If it persists, the health check is failing for another reason: open `https://<service>/actuator/health` and read which component is `DOWN` |
| Backend won't start, `Could not resolve placeholder 'REDIS_HOST'` | `SPRING_PROFILES_ACTIVE=prod` is set but `REDIS_HOST`/`REDIS_PASSWORD` are missing — set them, or unset the profile to use the in-process cache |
| Redis is misconfigured / unreachable but the app is up | Expected. The cache is an optimization: errors are logged and the call falls through to the database. Look for `Cache get failed on 'rolePermissions'` in the logs |
| Coverage/RAG returns a stub answer | `GEMINI_API_KEY` missing or out of quota — it degrades gracefully; add a working key for real answers |
| OCR returns nothing | `GOOGLE_VISION_API_KEY` unset, or **billing not enabled** on the Google project |

---

## 🔁 Redeploys

Both Render and Vercel **auto-deploy on every push to `main`** by default. Push code → they rebuild. Migrations run automatically on backend boot (each new migration is applied once, in order).

---

Local development instead? See **[setup.md](setup.md)** 🛠️
