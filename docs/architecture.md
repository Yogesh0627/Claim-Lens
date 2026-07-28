# ClaimLens - Architecture

Status: Frozen

Version: 1.0

Owner: Niyo Technologies

Last Updated: June 2026

**As-built (2026-07-29):** This document was written as the V1 design target. It has been reconciled against the shipped code — where the build diverged from the original design, the original intent is preserved and an **As-built** note records what was actually delivered. Verified stack: Java 21, Spring Boot 4 (4.0.6), Spring Security 7, Hibernate 7 / Spring Data JPA, PostgreSQL 17, Flyway V1–V32; Next.js 16 (App Router) + React 19 + Tailwind 4 + shadcn/ui frontend; Python FastAPI analysis-service.

---

# Purpose

This document defines the complete architecture of ClaimLens V1.

It covers:

* System Components
* Runtime Processing
* Service Communication
* Infrastructure Design
* Deployment Strategy
* Scalability Strategy
* Monitoring Strategy
* Failure Recovery

---

# Architecture Principles

ClaimLens follows:

1. Multi-Tenant SaaS Architecture
2. Modular Monolith Core
3. Service-Oriented Processing
4. Event-Driven Background Processing
5. API First Design
6. Auditability By Default
7. Horizontal Scalability

---

# Architecture Style

Core Platform

* Spring Boot Modular Monolith

Processing Components

* OCR Service
* Analysis Service

Storage Components

* PostgreSQL
* Redis
* S3

Observability Components

* Prometheus
* Grafana

Reason:

Business workflows remain centralized while computational workloads scale independently.

---

# System Components

1. Frontend
2. Backend API
3. Claim Processing Orchestrator
4. OCR Service
5. Analysis Service
6. PostgreSQL
7. Redis
8. S3
9. Prometheus
10. Grafana

---

# Frontend

Technology

* Next.js
* TypeScript
* Material UI

**As-built (2026-07-29):** Next.js 16 (App Router) + React 19 + TypeScript. The UI layer is **Tailwind CSS 4 + shadcn/ui (Radix primitives)**, not Material UI. State is Redux Toolkit (auth only) + axios; forms use react-hook-form (no zod); dates via dayjs. The listed "portals" are role-scoped views of a single app, not separate deployments.

Responsibilities

* Customer Portal
* Employee Portal
* Investigator Portal
* Manager Portal
* Admin Portal
* Dashboards
* Notifications

Scaling

Horizontal

---

# Backend API

Technology

* Spring Boot

**As-built (2026-07-29):** Java 21, Spring Boot 4 (4.0.6), Spring Security 7, Hibernate 7 / Spring Data JPA, PostgreSQL 17, Flyway migrations V1–V32. Packaged as a modular monolith with per-domain packages (`claim`, `document`, `fraud`, `processing`, `assignment`, `tenancy`, `security`, `auth`, `organization`, `policy`, `product`, `portal`, `coverage`, `audit`, `analytics`, …).

Responsibilities

* Authentication
* Authorization
* Tenant Isolation
* Claim Management
* Document Management
* Assignment Engine
* Investigation Workflow
* Fraud Rule Execution
* Notification Orchestration
* Audit Logging

The backend acts as the system orchestrator.

---

# Claim Processing Orchestrator

Purpose

Coordinate claim processing stages safely.

Responsibilities

* Track OCR Completion
* Track Analysis Completion
* Queue Fraud Jobs
* Prevent Duplicate Execution
* Manage Reprocessing
* Recover Failed Jobs

Implemented inside Spring Boot.

Separate service not required in V1.

---

# OCR Service

Technology

* Python
* EasyOCR
* Tesseract

**As-built (2026-07-29):** OCR is not a standalone EasyOCR service. It sits behind an interface with an **OFFLINE NoOp default**; the switching flag `OCR_ENABLED` plus `OCR_PROVIDER` selects **Google Cloud Vision (in-JVM, `=vision`)** or an optional **Python HTTP OCR service (`=http`, Tesseract-based)**. Production runs Vision.

Responsibilities

* OCR Processing
* Text Extraction
* Structured Data Extraction

Scales independently.

---

# Analysis Service

Technology

* Python
* OpenCV

**As-built (2026-07-29):** FastAPI service (OpenCV + imagehash + Pillow), deployed separately on Render. Behind an interface with an OFFLINE NoOp default; enabled by `ANALYSIS_ENABLED` with `ANALYSIS_SERVICE_URL`, and authenticated to the backend by a shared secret (`ANALYSIS_SHARED_SECRET`).

Responsibilities

* Duplicate Image Detection
* Similarity Detection
* Metadata Analysis
* Fraud Signal Generation

Scales independently.

---

# Communication Strategy

Two communication models:

Synchronous

Used for:

* Login
* Claim Creation
* Claim Submission
* Assignment Acceptance
* Investigation Updates

Asynchronous

Used for:

* OCR
* Image Analysis
* Fraud Analysis
* Notification Delivery
* Analytics Aggregation

---

# Claim Submission Flow

Customer
↓
Submit Claim
↓
Backend Validation
↓
Generate Claim Number
↓
Store Claim
↓
Store Documents
↓
Create OCR Jobs
↓
Create Analysis Jobs
↓
Create ClaimProcessingState
↓
Status = AWAITING_ANALYSIS
↓
Return Success

Customer never waits for OCR or Fraud Analysis.

---

# Claim Processing State

Table:

claim_processing_state

Fields

* claim_id
* ocr_status
* analysis_status
* fraud_status
* pending_reprocess
* last_updated_at

Purpose

Track processing lifecycle.

---

# OCR Flow

Worker Claims Job
↓
FOR UPDATE SKIP LOCKED
↓
Download Document Version
↓
OCR Processing
↓
Store OCR Metadata
↓
Store OCR Field Extractions
↓
Update ClaimProcessingState
↓
OCR Complete

---

# Analysis Flow

Worker Claims Job
↓
FOR UPDATE SKIP LOCKED
↓
Download Images
↓
OpenCV Processing
↓
Duplicate Detection
↓
Similarity Detection
↓
Metadata Extraction
↓
Store Results
↓
Update ClaimProcessingState
↓
Analysis Complete

---

# Fraud Trigger Strategy

Only Orchestrator can queue Fraud Jobs.

OCR and Analysis services never trigger Fraud directly.

Atomic Update:

Fraud queued only when:

* OCR Complete
* Analysis Complete
* Fraud Not Started

Result:

Exactly One Fraud Job Per Claim

No Race Conditions

No Duplicate Execution

**As-built (2026-07-29):** The atomic gate keys on a conditional UPDATE plus a partial-unique `fraud_job` row, and it settles when both OCR and analysis reach a **TERMINAL** state — succeeded OR retries-exhausted — so a failing document can never hang the gate. The gate never fires until both are terminal.

---

# Claim Snapshot Builder

Purpose

Build current claim state before fraud execution.

Example:

Doc1 → V1

Doc2 → V1

Doc3 → V1

Doc4 → V1

Doc5 → V2

↓

Current Claim Snapshot

↓

Fraud Engine

This avoids reprocessing unchanged documents.

---

# Fraud Analysis Flow

Load Fraud Policy
↓
Load Fraud Rules
↓
Build Claim Snapshot
↓
Execute Rules
↓
Generate Fraud Score
↓
Generate Fraud Alerts
↓
Update Claim
↓
Queue Assignment

**As-built (2026-07-29):** `FraudEngine.evaluate()` runs weighted `FraudRule`s → a **0–100 score** bucketed **LOW / MEDIUM / HIGH**, and is **explainable** — each rule's contribution is persisted to `fraud_rule_execution`. On completion the claim moves to `AWAITING_ASSIGNMENT`.

---

# Assignment Flow

Load Assignment Policy
↓
Find Eligible Investigators

Filters:

* Same Tenant
* Active
* Correct Region
* Correct Branch
* Not On Leave

↓

Apply Strategy

* Round Robin
* Least Loaded

↓

Create Assignment
↓
Notify Investigator
↓
Status = AWAITING_ACCEPTANCE

---

# Assignment Acceptance

Investigator
↓
Accept Assignment
↓
Create Investigation
↓
Status = UNDER_INVESTIGATION

---

# Assignment Rejection

Investigator
↓
Reject Assignment
↓
Assignment History
↓
Reassignment Triggered
↓
New Assignment

---

# Additional Information Flow

Investigator
↓
Request Documents
↓
WAITING_FOR_CUSTOMER

Customer
↓
Upload New Version
↓
DocumentVersion Created

Only new document versions are processed.

---

# Incremental Reprocessing

Example

Claim contains:

Doc1 V1
Doc2 V1
Doc3 V1
Doc4 V1
Doc5 V1

Customer uploads:

Doc5 V2

Processing:

OCR(Doc5 V2)

Analysis(Doc5 V2)

Rebuild Snapshot

Fraud Recalculation

No processing of Docs 1–4.

---

# Reprocessing Protection

ClaimProcessingState contains:

pending_reprocess

If processing already running:

pending_reprocess = TRUE

No duplicate processing jobs created.

When active processing completes:

One reprocessing cycle executes.

---

# Upload Guardrails

Before OCR:

Validate

* File Type
* File Size
* Page Count
* Corruption Check

Reject unsupported files before worker consumption.

---

# Tenant Rate Limiting

Redis

Examples

tenant:uploads_per_minute

tenant:ocr_requests_per_hour

tenant:analysis_requests_per_hour

Protects platform stability.

**As-built (2026-07-29):** Rate limiting is per-IP / per-user (not per-tenant) across three buckets: `auth` (20/min per IP per URI), `ai` (30/min per user on `/coverage*` + `/knowledge`), and `upload` (60/min per user on POST `/documents`). Backing store is **Redis/Upstash under the `prod` profile, in-memory in dev**. The limiter **fails open** (Redis unreachable → allow), and bucket keys use the leftmost `X-Forwarded-For` — a spoofable header, tracked as a known gap (see docs/audit-report.md, H2).

---

# Pagination

**As-built (2026-07-29):** Added this session; not in the original design. List endpoints (`GET /claims`, `/customers`, `/users`, `/policies`) return a `PagedResponse<T>` `{content, page, size, totalElements, totalPages, first, last}` and accept `?page&size` (clamped to page ≥ 0, size 1..100 by `PageRequests`). Each gained a sibling **`/options`** endpoint (unpaged, for pickers/dropdowns); `/users/options?role=` replaced the old role filter on `/users`. Frontend consumes these via a `usePaginated` hook (auto-clamps to the last page after a delete so lists never show a blank page) and a shared `pagination-bar` component.

---

# PostgreSQL Architecture

Database

claimlens_db

Strategy

Single Shared Database

Tenant Isolation

tenant_id

All business entities contain:

tenant_id

PostgreSQL remains the source of truth.

**As-built (2026-07-29):** Isolation is enforced by a **Hibernate `@TenantId` discriminator** on `TenantAwareEntity` (global entities — Role, Permission, and the tenant-root InsuranceCompany — extend `BaseEntity` instead). `ClaimLensTenantIdentifierResolver.isRoot()` is hardcoded `false`, and an unbound context resolves to `TenantContext.SYSTEM_TENANT = -1L` (matches no real rows). `TenantContext` is a plain `ThreadLocal` **cleared in a `finally`**. A cross-tenant read/write by direct id returns **404** (no existence oracle) — verified live against a second tenant.

---

# Database Schemas

public

* claim
* assignment
* investigation
* customer
* user

processing

* ocr_job
* analysis_job
* fraud_job
* notification_job
* claim_processing_state
* ocr_result

audit

* audit_event
* audit_log

analytics

* analytics_snapshot
* dashboard_metric

**As-built (2026-07-29):** All tables live in the single `public` schema (organized by module, not separate Postgres schemas), created by Flyway V1–V32. Investigation is a lightweight `InvestigationNote` — the originally-planned `FraudCase`, `InvestigationFinding`, `InvestigationEvidence`, and `AuditAttachment` entities were **not built**. Analytics is computed on read rather than from materialized `analytics_snapshot`/`dashboard_metric` tables.

---

# Job Processing Strategy

Job Tables

* ocr_job
* analysis_job
* fraud_job
* notification_job

Worker Claims Job

Using:

FOR UPDATE SKIP LOCKED

Benefits

* No Duplicate Processing
* Safe Parallel Execution

---

# Worker Crash Recovery

Job Status

* PENDING
* PROCESSING
* COMPLETE
* FAILED

Fields

* locked_at
* attempt_count

Recovery Worker

Find:

PROCESSING

Older Than Threshold

↓

Reset To PENDING

↓

Retry

No Lost Jobs.

---

# Redis Architecture

Used For

* Caching
* Rate Limiting
* Assignment Locks
* Dashboard Caching

Redis is never a source of truth.

**As-built (2026-07-29):** Redis (Upstash) is engaged only under the `prod` profile — for the permission cache and the rate-limiter. In dev the same interfaces fall back to **Caffeine (cache) / in-memory (rate limit)**, so Redis is not required to run locally. Assignment concurrency is handled in Postgres (`FOR UPDATE SKIP LOCKED`), not Redis locks.

---

# S3 Architecture

Stores

* Documents
* Images
* Evidence
* Reports
* OCR Payloads

Database stores metadata only.

**As-built (2026-07-29):** Storage is behind an interface with a **local-filesystem OFFLINE default**; `STORAGE_PROVIDER=s3` switches to any S3-compatible backend — production uses **Cloudflare R2**. Downloads are hardened by `SafeDownloads` (see Security Architecture / audit-report.md).

---

# OCR Storage Strategy

Large OCR payloads are not stored inside PostgreSQL.

Example:

ocr-result.json

↓

S3

Database stores:

* payloadLocation
* confidenceScore
* processingTimeMs

Benefits

* Lower Disk IO
* Faster Queries
* Smaller Tables

---

# Monitoring Architecture

Micrometer
↓
Prometheus
↓
Grafana

Metrics

* API Latency
* Request Count
* Error Rate
* JVM Metrics
* DB Pool Metrics
* OCR Processing Time
* Analysis Processing Time

**As-built (2026-07-29):** Spring Boot Actuator is wired and exposes the health endpoint used for platform health checks; a dedicated Prometheus + Grafana stack is **not deployed** in the V1 hosted setup (Render/Vercel provide their own platform metrics). Treat this section as the intended metrics model rather than a running dashboard.

---

# Logging Architecture

Structured JSON Logs

Fields

* timestamp
* traceId
* requestId
* tenantId
* userId
* serviceName

Benefits

* Traceability
* Debugging
* Audit Support

---

# Health Checks

Every Service Exposes

/health

Checks

* PostgreSQL
* Redis
* S3

Used by:

* Monitoring
* Load Balancer

**As-built (2026-07-29):** The backend health check is Actuator's **`/actuator/health`** (this is the path Render probes in `render.yaml`); the Python analysis-service exposes `/health`.

---

# Failure Recovery

OCR Failure
↓
Retry
↓
Manual Review Queue

Analysis Failure
↓
Retry
↓
Manual Review Queue

Notification Failure
↓
Retry
↓
Dead Letter Queue

**As-built (2026-07-29):** Retries are bounded; when a job exhausts them it lands in a **terminal (retries-exhausted)** state rather than a separate broker-backed DLQ/manual-review queue. Critically, a permanently-failing OCR/analysis job is still *terminal*, so the fraud gate settles and the claim never hangs. Notifications are delivered by **synchronous in-process calls** from the claim service to `NotificationService` (an in-app row + best-effort email via the `EmailSender` provider) — there is **no** message broker and **no** transactional-outbox layer in V1 (the `events`/`outbox` packages are empty placeholders; no outbox table exists).

---

# Scalability Strategy

Frontend

Horizontal Scaling

Backend

Horizontal Scaling

OCR Service

Independent Scaling

Analysis Service

Independent Scaling

Database

Vertical Scaling Initially

Read Replicas Later

Redis

Single Instance V1

Cluster Future

---

# Security Architecture

* JWT Authentication
* RBAC
* Tenant Isolation
* Password Hashing
* HTTPS
* Audit Logging
* Secure S3 Access

**As-built (2026-07-29):**

* **JWT** — JJWT, **HS256 with the algorithm pinned on parse** (`verifyWith(key)`), so `alg:none`/algorithm-confusion is rejected 401. The secret has no default (fail-fast). The token carries `{sub, tid, rid, …}` — the **roleId, not permissions**. Permissions are resolved **server-side per request** from `role_permission` (+ per-tenant `tenant_role_permission` overrides) and cached (Caffeine dev / Redis prod).
* **Refresh tokens** — 256-bit SecureRandom, **SHA-256 hashed at rest**, single-use, **rotated on use**. Invitation/reset tokens are 32-byte SecureRandom, SHA-256 at rest, single-use, TTL'd (1h reset / 7d invite).
* **RBAC** — 11 global roles; permission checks are code-based (module `_READ`/`_WRITE`/etc.) evaluated against the resolved permission set. The customer portal adds an **ownership gate** (a customer can only reach their own claims/policies).
* **Tenant Isolation** — Hibernate `@TenantId` discriminator (see PostgreSQL Architecture); cross-tenant access returns 404.

**Security posture — audit-verified (full detail in [docs/audit-report.md](audit-report.md)):**

* **Held under direct attack:** tenant isolation, JWT parsing (no `alg:none`/confusion), the customer-portal ownership gate, SQL-injection resistance, and path-traversal resistance.
* **Fixed this pass:** the **critical stored-XSS on document download** (now guarded by `SafeDownloads` — allowlisted image/pdf served inline, everything else `octet-stream` + `attachment`); 405/415/413 error handlers (were 500s); explicit upload limits (10 MB file / 15 MB request); `/roles` authz (`USER_READ`); catch-all now logs; the `/error-test` debug endpoint removed.
* **Open / documented tradeoffs:** `X-Forwarded-For` rate-limit bypass, unaudited platform-admin impersonation, AI deny-list authz, no session-revoke on password change, in-tenant product-doc IDOR, 6-char password minimum, and the 15-min access-token revocation window.

---

# Deployment Target

AWS

Services

* EC2
* RDS PostgreSQL
* ElastiCache Redis
* S3
* Application Load Balancer

Deployment

Docker Containers On EC2

No Kubernetes In V1

**As-built (2026-07-29):** V1 ships on a managed PaaS topology, not self-hosted AWS. A Render blueprint (`render.yaml`) deploys two Docker services — the **Spring Boot backend** and the **FastAPI analysis-service** (both `region: singapore`, free plan, health-checked). The rest is external managed services:

* **Frontend** → Vercel
* **Database** → Neon PostgreSQL (external, DIRECT endpoint; pgvector for RAG)
* **Cache / rate-limit** → Upstash Redis (`prod` profile)
* **Object storage** → Cloudflare R2
* **Email** → Resend
* **OCR** → Google Vision · **AI/RAG** → Gemini

`DEMO_SEED=true` seeds a "Demo Insurance" tenant. Full steps in deploy.md. The EC2/RDS/ElastiCache/ALB layout above remains the reference self-hosted design.

---

# Future Evolution

V2

* RabbitMQ
* Kafka
* Read Replicas
* Advanced Fraud AI

V3

* Kubernetes
* Multi Region Deployment
* Event Streaming
* ML Fraud Models

---

# Architecture Summary

Users
↓
Load Balancer
↓
Frontend
↓
Backend API
↓
Claim Processing Orchestrator

Backend
↓
PostgreSQL

Backend
↓
Redis

Backend
↓
S3

Orchestrator
↓
OCR Workers

Orchestrator
↓
Analysis Workers

Orchestrator
↓
Fraud Workers

Prometheus
↓
Grafana

ClaimLens V1 is designed as a scalable, multi-tenant, production-ready insurance claim investigation platform with strong auditability, fault tolerance, and operational observability.
