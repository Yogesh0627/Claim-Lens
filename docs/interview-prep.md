# ClaimLens — Interview Preparation Guide

A living study guide for the ClaimLens project. Structured **easy → hard**. It captures what we
built, **why** we chose it, the trade-offs we weighed, and the concrete problems we hit and fixed.
Update it whenever we make a decision or fight a bug — the "war stories" (Part 10) are what
interviewers remember.

> Status legend used below: ✅ built & tested · 🔜 planned · ⚠️ deferred/simplified for V1

---

## How to use this doc

- If you have 10 minutes before an interview: read Part 1, Part 5 (multi-tenancy), and Part 10 (war stories).
- If asked "walk me through the project": use Part 1 → Part 4.
- If asked "tell me about a hard technical problem": pick any story from Part 10.
- Every decision here has a **why** and a **trade-off**. Interviewers probe the *why*, not the *what*.

---

## Part 1 — The 30-second pitch (EASY)

**What is it?** ClaimLens is a multi-tenant SaaS platform for **motor insurance claims**. A customer
submits a claim with documents; OCR and image analysis run in the background; a rule-based fraud
engine scores it; an investigator is auto-assigned and approves or rejects — with a full audit trail.

**One-liner:** "Automation prepares the decision; a human makes it." (Tagline: *Investigate Faster.
Decide Smarter.*)

**Who uses it?** Insurance companies (each is an isolated *tenant*), with roles like Company Admin,
Investigation Manager, Investigator, Employee, and Customer.

**Why it matters (the problem):** insurers face manual investigation, fraud/duplicate claims, slow
turnaround, uneven investigator workloads, and weak auditability. ClaimLens standardizes and
automates the pipeline while keeping humans in control of the final call.

---

## Part 2 — Domain & business context (EASY–MEDIUM)

You'll sound credible if you know the insurance vocabulary. The key relationships:

```
InsuranceCompany (the tenant / insurer)
  └─ InsuranceProduct           e.g. "Private Car Premium"   (what the insurer SELLS)
       └─ InsuranceProductVersion  v1, v2, v3                (terms change over time)
            └─ ProductDocument      the policy-wording PDF    (feeds the AI/RAG layer later)

Customer (the policyholder)
  └─ InsurancePolicy            e.g. MOT-2024-88123           (the CONTRACT they bought)
       ├─ pinned to a specific InsuranceProductVersion        (their terms, frozen)
       ├─ covers an InsuredVehicle (reg no, make, chassis…)
       └─ valid effective_from → effective_to, with a sum_insured
            └─ Claim                                          (what happened — an accident)
```

**Three things people confuse — be precise:**
1. **Product** = the template the insurer sells (coverage, exclusions), versioned over time.
2. **Policy** = the actual contract a customer bought (a specific product version, a specific vehicle,
   validity dates, sum insured).
3. **Claim** = a request for payment against a policy after an incident.

**Claim lifecycle (the state machine):**
`DRAFT → SUBMITTED → AWAITING_ANALYSIS → AWAITING_ASSIGNMENT → AWAITING_ACCEPTANCE →
UNDER_INVESTIGATION → (WAITING_FOR_CUSTOMER) → APPROVED | REJECTED → CLOSED`, plus `REOPENED`.

> Interview Q: *"Why so many states?"* — Because each is a distinct hand-off point (analysis done,
> assignment made, investigator accepted, waiting on the customer). Fewer states would hide where a
> claim is stuck, which matters for SLAs and dashboards.

---

## Part 3 — Tech stack & why (EASY–MEDIUM)

| Layer | Choice | Why |
|---|---|---|
| Backend | **Spring Boot 4 (Java 21)** | Mature ecosystem, strong transactions, one team's language |
| DB | **PostgreSQL 17** | ACID, JSONB, partial indexes, `FOR UPDATE SKIP LOCKED`, pgvector later |
| Migrations | **Flyway** | Versioned, immutable, single source of truth for schema |
| Auth | **JWT (JJWT 0.12)** | Stateless, horizontally scalable |
| Cache | **Redis (Upstash) / Caffeine** | Permission-resolution cache — Caffeine in dev, Redis in prod, same code |
| Object storage | **S3 / R2 (MinIO locally)** | Documents & images; DB stores metadata only |
| Async workers | **Python** (OCR + image analysis) | CPU-heavy work that scales independently |
| Observability | Micrometer → Prometheus → Grafana | Metrics, dashboards |
| Frontend | Next.js + TypeScript + MUI | Six role-based portals (planned) |

> Interview Q: *"Why Postgres and not MongoDB?"* — Claims are highly relational (policy → claim →
> documents → investigation → fraud score) and need strong transactional integrity for money
> decisions and audit. Postgres also gives us `SKIP LOCKED` job queues and pgvector for the AI layer,
> so we avoid extra infrastructure.

---

## Part 4 — Architecture (MEDIUM)

**Style: a modular monolith with two satellite Python workers.**

```
Users → Load Balancer → Frontend → Backend API (Spring Boot modular monolith)
                                       │
                    ┌──────────────────┼─────────────────┐
                 PostgreSQL          Redis                S3
                                       │
                          Claim Processing Orchestrator (inside the monolith)
                                       │
                    ┌──────────────────┼──────────────────┐
                OCR worker        Analysis worker      Fraud worker
                 (Python)           (Python)          (inside monolith)
```

**Why a modular monolith, not microservices?**
- Business workflow (claim state, tenancy, audit) stays in **one transactional boundary** with one
  owner — simpler and safer than distributed transactions across services.
- Only the **CPU-heavy** work (OCR via Tesseract/Vision, image analysis via OpenCV) is split out,
  because it scales on a *different axis* than the business logic.
- V1 explicitly avoids Kafka/RabbitMQ/Kubernetes — job queues live in Postgres (`FOR UPDATE SKIP
  LOCKED`), which is a coherent, broker-free design at this scale.

**Why the folder structure is "package-per-module":** each domain (`claim`, `document`, `fraud`, …)
has an identical internal skeleton (`controller/service/repository/entity/dto/mapper/…`). Modules own
their tables and talk via events, not by reaching into each other's repositories. That uniformity
means the seams are already cut — a module can later become its own service without a rewrite. The
cross-cutting packages (`common`, `config`, `security`, `tenancy`, `events`, `outbox`) sit *outside*
the domain modules and never depend back on them.

> Interview Q: *"When would you split a module into a real microservice?"* — When it needs
> independent scaling or deployment cadence, or a different datastore. The event-based seams and
> per-module table ownership are what make that a controlled change instead of a big-bang rewrite.

---

## Part 5 — Multi-tenancy deep dive (MEDIUM–HARD) ⭐ the headline topic

**The model: shared database, shared schema, `tenant_id` discriminator.** Every tenant-scoped table
has a `tenant_id` column; `InsuranceCompany` is the tenant root.

**The problem we were solving:** originally the tenant arrived as a `companyId` *path variable* from
the client, and there was no auth — so **any caller could pass any `companyId` and read another
tenant's data** (a classic IDOR / cross-tenant leak). ✅ Fixed.

### The decision that matters most: how to enforce tenancy

We evaluated three mechanisms:

| | Manual repo params | Hibernate `@Filter` | **Hibernate `@TenantId`** ✅ |
|---|---|---|---|
| Applies to `findById()` | only if hand-written | **NO** | **YES** |
| Applies to JPQL/derived queries | only if hand-written | yes (when enabled per session) | yes |
| Auto-set on INSERT | no | no | **yes** |
| Can be forgotten? | yes (silent leak) | yes (silent leak) | **no** |

**Why `@TenantId` won — the killer argument:** `@Filter` is **not applied to
`EntityManager.find()` / `findById()`**. Our entire codebase loads by id then compares tenant
manually. `@Filter` would leave exactly that path unguarded while *looking* protected. `@TenantId`
enforces inside Hibernate's entity loader — the discriminator is appended to SELECT/UPDATE/DELETE
**including load-by-id**, and set automatically on INSERT from the current tenant. There's no
per-call opt-in, so it can't be forgotten.

### How it's wired (the mechanism)

```
Request → JwtAuthenticationFilter (sets principal, incl. tenantId from JWT)
        → TenantFilter (reads principal → TenantContext.set(tenantId), clears in finally)
        → controller → service → repository
                                    │
             Hibernate asks ClaimLensTenantIdentifierResolver for the current tenant
                                    │
             resolver returns TenantContext.getTenantId()  (or SYSTEM_TENANT = -1 if unset)
                                    │
             every query gets  ... AND tenant_id = ?   automatically
```

- **`TenantContext`** — a `ThreadLocal<Long>`. **Not** `InheritableThreadLocal` (that leaks across
  pooled threads and gives `@Async` children a stale tenant). Cleared in the filter's `finally`
  because Tomcat reuses threads — a leaked tenant would bleed into the next request.
- **`TenantAwareEntity`** — a `@MappedSuperclass` with the `@TenantId` field. Entities either extend
  it (protected) or extend `BaseEntity` (deliberately global: `InsuranceCompany`, `Role`,
  `Permission`). Tenancy is a **type-level** property.
- **Resolver never returns null** — Hibernate would throw and break startup/login. It falls back to
  `SYSTEM_TENANT = -1L`, which matches no real tenant's rows.
- **`isRoot()` is pinned to `false`.** Hibernate skips the tenant restriction entirely for a "root"
  tenant — a built-in backdoor we never want wired to a super-admin flag by accident.

### The proof (why we trust it)

An integration test seeds two tenants' regions via **raw JDBC** (bypassing the filter, so the test
proves the *reader* is filtered, not the writer), then as tenant A:
- `GET /regions/{B's region}` → **404** (not 200, not 403 — 403 would confirm existence).
- `GET /regions` → only A's regions.
- Create a region → Hibernate stamps `tenant_id = A`; tenant B can't see it.

> Interview Q: *"Why return 404 and not 403 for another tenant's resource?"* — 403 says "this exists
> but you can't have it," which leaks existence and lets an attacker enumerate ids. 404 says nothing.
>
> Interview Q: *"Trade-offs of shared-schema multi-tenancy?"* — Cheapest to operate and onboard, but
> a single bug can cross tenants, so enforcement must be un-forgettable (hence `@TenantId`), and noisy
> neighbors share resources. Alternatives: schema-per-tenant (more isolation, harder migrations) or
> DB-per-tenant (most isolation, most ops cost). For V1 SaaS scale, shared schema + `@TenantId` +
> (future) Postgres row-level security is the right point on the curve.
>
> Interview Q: *"What's the escape hatch that could still leak?"* — Native SQL queries bypass
> `@TenantId`. We ban them on tenant-scoped entities (enforced by a test), and Postgres RLS is the
> belt-and-braces option for later.

---

## Part 6 — Security & authentication (MEDIUM–HARD)

**Stateless JWT.** The filter chain order is load-bearing: **JwtAuthenticationFilter → TenantFilter**,
because the tenant is derived from the authenticated principal.

### Decision: JWT carries `roleId`, NOT a `permissions[]` array

The original design put a full permission array in the token. We rejected that:
1. **Revocation:** a signed token can't be clawed back. Revoke `CLAIM_APPROVE` and the user keeps it
   until the token expires. For a system approving real money, minutes of stale authority is an audit
   finding.
2. **Size:** the permission catalogue grows to 100+ codes, in every request header.
3. **Drift:** it duplicates `role_permission`, which is already the source of truth.

**What we do instead:** the JWT carries `{ sub, tid (tenant), rid (role), emp, exp, jti }`.
`PermissionService` resolves the role's permission codes **server-side** per request (one indexed
join), and those become the principal's authorities. Result: near-JWT performance with **immediate**
revocation. That resolution is now **cached** (`rolePermissions`, keyed by roleId) — Caffeine
in dev, Redis (Upstash) in prod, through Spring's cache abstraction so the service code names no
provider. Role→permission mappings are migration-managed (the V6 seed), so a change is a redeploy
that clears the cache; a 10-minute TTL is the backstop, and caching is off under the test profile so
the RBAC gate test still proves the uncached "revoke → denied next request" path.

### RBAC enforcement

- `@EnableMethodSecurity` + `@PreAuthorize("hasAuthority('ORG_REGION_WRITE')")` on the **service
  layer** (not controllers — controllers can be bypassed by internal callers, and business logic
  doesn't belong there).
- Permission **codes**, not role names, so the 9 seeded roles stay reconfigurable via `role_permission`
  without a code change.
- A seed migration maps roles → permissions (`TENANT_ADMIN` = full org access; `AUDITOR` = read-only).
  Without the seed, every `@PreAuthorize` denies — RBAC is inert until permissions exist.

### Other hardening

- **Algorithm pinning:** we verify with the HMAC key, so an `alg: none` (unsigned) token is rejected.
  Never trust the token's own `alg`. (Tested: `alg:none` → 401.)
- **Roles are global** (not per-tenant) — matches the shipped schema and the 9 seeded system roles.
- Passwords will be BCrypt-hashed; refresh tokens will be stored as SHA-256 hashes with rotation +
  theft detection (revoke the whole family on reuse). 🔜 (comes with the login module in Phase 3)
- **Login resolves the tenant** because email is **globally unique** (`UNIQUE(email)`), so
  `{email, password}` uniquely identifies a user and thus their tenant.

> Interview Q: *"Stateless JWT vs server sessions?"* — JWT scales horizontally (no shared session
> store) but you trade away easy revocation; we buy revocation back by keeping *permissions* server-side
> and keeping access-token TTL short.

### Caching — one abstraction, two backends

Permission resolution runs on **every** authenticated request, so it's the natural first cache. The
design:

- **Spring's cache abstraction (`@Cacheable`), not a Redis client in the service.** `PermissionService`
  is annotated `@Cacheable("rolePermissions")`; it names no provider. The backend is chosen by profile:
  **Caffeine** (in-process) in local dev, **Redis (Upstash)** under the `prod` profile. Swapping the
  backend is a config change, not a code change.
- **Why Caffeine locally, Redis in prod?** Upstash lives in the cloud. A local dev process reaching
  across the network to it would cost *more* than the single indexed join the cache is meant to save.
  In prod the app sits next to Redis, and the cache is shared across instances.
- **One Redis account, many apps → namespaced keys.** Every key ClaimLens writes is prefixed
  `claimlens:` (`CacheConfig.prefixCacheNameWith`), so the same Upstash database can be shared with
  other projects without collision. Values are JSON-serialized (readable in the Upstash console,
  survive a class moving package) with **polymorphic typing restricted to our own package** — an
  unrestricted deserializer on a shared cache is an RCE vector.
- **The eviction question (the interesting part).** Caching permissions seems to fight D11's "revoke
  takes effect immediately." It doesn't, because ClaimLens has **no runtime permission-mutation
  endpoint** — role→permission mappings are migration-managed (the V6 seed). A change is therefore a
  redeploy, which clears the cache; a 10-minute TTL is the backstop. If a runtime
  permission-management screen is ever added, it carries one `@CacheEvict(value="rolePermissions")`.
- **Cache is off in tests** (`spring.cache.type=none`), so `RbacIntegrationTest` — which revokes a
  permission by deleting a `role_permission` row directly and asserts the next request is denied —
  tests the *live* D11 guarantee, not a cached copy.

> Interview Q: *"Isn't caching auth data dangerous?"* — only if the cached thing can change out from
> under you at runtime. Here it can't (permissions are seed data); the invalidation boundary is the
> deploy. State that boundary explicitly and the cache is safe. The moment a runtime editor lands,
> the boundary moves to that write, and eviction moves there with it.

### Rate limiting — the second thing Redis buys us

`/auth/**` is the only unauthenticated surface in the API, which makes it the only brute-forceable
one. It's now rate limited per client IP, per endpoint, in a fixed window (default 20/min).

- **Two implementations behind one interface**, same swappable shape as storage/OCR/email/cache:
  in-memory for dev and tests, **Redis for prod**. The distinction matters: an in-memory counter is
  *per instance*, so behind a load balancer the real limit silently becomes `limit × instances`.
  Redis `INCR` is atomic and shared, so the limit is the limit.
- **It fails OPEN.** If Redis is unreachable the request is allowed and a warning is logged. A rate
  limiter blunts abuse; letting it lock every user out during an infra blip trades a small risk for
  a total outage — the same lesson the cache taught us on the first deploy.
- **Keyed on `X-Forwarded-For`**, not `getRemoteAddr()`: behind Render's proxy the socket address is
  the load balancer, so every user in the world would share one bucket.
- **Per-endpoint buckets**, so someone hammering `/login` can't also lock out `/refresh-token` for
  legitimate sessions. A test asserts exactly that.
- **The filter is not a `@Component`** — it's constructed in `SecurityConfig`. A `Filter` *bean* gets
  auto-registered by Boot on the raw servlet chain **as well**, so it would run twice and
  double-count every request.

> War story: `addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)` failed at startup with
> *"JwtAuthenticationFilter does not have a registered order."* Spring Security can only position a
> filter relative to one it has **already** registered, and I'd put my line above the one that
> registers the JWT filter. Fix: register the JWT filter first, then anchor to it. The final order is
> rate-limit → authenticate → resolve tenant, which is also the order you want on cost grounds —
> a throttled request is rejected before any password hashing or database work.

### Connection pooling

- **HikariCP** (Spring Boot's default) is explicitly tuned rather than left on defaults: small pool
  (max 10) because **Neon's free tier caps direct connections** — one Render instance × 10 stays under
  the limit. `pool-name`, `minimum-idle`, and the timeout/lifetime knobs are all env-overridable.
- A common confusion worth pre-empting: Hibernate logs `Pool: DataSourceConnectionProvider … pool
  size: undefined/unknown`. That's Hibernate saying it *delegates* to the Spring-managed DataSource —
  **not** an absence of pooling. Hikari owns the pool (`HikariPool-1 - Starting…` in the boot log).

### The login / refresh flow (built in Phase 3)

- **Login** (`POST /auth/login {email, password}`): look up the user by email (a **native** query,
  because at login the tenant is unknown — see below), verify the BCrypt hash, then issue a short-lived
  **access token** (JWT) + a long-lived **refresh token**.
- **Refresh token = opaque random string, stored hashed.** We generate 32 random bytes (`SecureRandom`,
  Base64URL), return the raw token to the client, and store only its **SHA-256 hash** in `user_session`.
  A leaked database never yields usable refresh tokens.
- **Rotation + theft detection:** each refresh **revokes the old session and issues a new one**, linked
  via `replaced_by_session_id`. Reusing an already-rotated (revoked) token is rejected — a theft signal.
- **Why authentication queries bypass `@TenantId`:** login and refresh happen **before** we know the
  tenant (we resolve the tenant *from* the user; email is globally unique). Normal `@TenantId` queries
  would filter to "no tenant" and find nothing. So these specific lookups use **native SQL** that isn't
  rewritten with the tenant discriminator. This is the same reason the mid-transaction `TenantContext`
  trick fails (see war story #10) — the tenant is fixed when the Session opens.

> Interview Q: *"Why not store the refresh token itself?"* — Same reason you hash passwords: a DB
> compromise shouldn't hand over live credentials. We store the hash and compare hashes.

---

## Part 7 — Data modeling decisions (MEDIUM–HARD)

### Product → Version → Policy → Claim, and "version pinning"

A claim stores **both** `insurance_product_id` and `insurance_product_version_id`. Why? Because a
customer who bought *Premium v2* in January and crashes in August — after v3 shipped — is covered by
**v2**, the terms they contracted under.

**The subtle bug we caught in our own design:** an early doc said "pin the claim to the product's
*currently active* version." That becomes **wrong** the moment a real `InsurancePolicy` exists:
pinning to v3 would adjudicate the claim against terms the customer never agreed to (a
mis-selling / regulatory issue), and it would be *invisible in testing* because V1-era data only has
one version. **Correct rule:** the claim inherits the version from the **policy** (pinned at sale),
never from the product's current state.

### `InsuredVehicle` as its own table (not columns on the policy)

Reasons: (1) `insurance_policy` is claim-type-agnostic — vehicle columns would be dead NULLs for
future health/property products; (2) commercial policies are fleet policies (many vehicles);
(3) duplicate-claim fraud rules need `(tenant_id, chassis_number)` as a first-class index across
policies. A partial unique index enforces one active vehicle per motor policy in V1.

### Snapshots vs joins

`claim.policy_number` and `claim.vehicle_*` are **write-once snapshots** ("what the claimant told us,
what was printed on the policy"). Rule: `insurance_policy_id` is the only thing you may join/filter on;
the snapshot columns are display/audit only. This survives later corrections and removes a join from
the highest-traffic query.

### Soft delete vs append-only

Decision rule: *a row gets soft delete **iff a user can retire it**; a row is append-only **iff it
records something that happened**; nothing is both.* An audit log with an `is_deleted` flag isn't an
audit log. ⚠️ We deferred physically splitting `BaseEntity` into `SoftDeletableEntity` /
`ImmutableEntity` until the first append-only entity arrives (nothing is lost by waiting).

**Corollary bug in most designs:** on a soft-deletable table, a plain `UNIQUE(code)` is a bug — a
soft-deleted row permanently blocks reuse of its code. Every unique index must be **partial**:
`... WHERE is_deleted = FALSE`.

### IDs: BIGSERIAL PK + `public_id UUID` for external URLs

We use `BIGSERIAL` primary keys (fast, compact FKs) and add a `public_id UUID` to externally-
addressable aggregates so URLs can be made opaque later without a PK migration. Enumeration is a
non-issue for a fully-authenticated B2B app *provided* cross-tenant reads return 404.

---

## Part 8 — Async processing & the fraud pipeline (HARD)

This is the most technically interesting part — great for a "design a reliable pipeline" question.

### The requirement

When a claim is submitted, OCR and image analysis run in the background. Only when **both** finish
should **exactly one** fraud job run. The customer never waits.

### `claim_processing_state` + the exactly-once fraud gate

One row per claim (`UNIQUE(claim_id)`) tracks `ocr_status`, `analysis_status`, `fraud_status`. Both
the OCR-completion and analysis-completion handlers call the same `tryQueueFraud(claimId)`, and they
**race on purpose**:

```sql
UPDATE processing.claim_processing_state
   SET fraud_status = 'QUEUED'
 WHERE claim_id = :id AND ocr_status = 'COMPLETE' AND analysis_status = 'COMPLETE'
   AND fraud_status = 'NOT_STARTED' AND pending_reprocess = FALSE;
```

Postgres row-locks that single row. The winner flips the gate and (in the **same transaction**)
inserts the fraud job. The loser blocks, re-reads the committed state, sees `QUEUED`, and matches
zero rows → does nothing. No advisory locks, no Redis, no race. A **partial unique index** on the job
table (`WHERE status IN ('PENDING','PROCESSING')`) is a second layer so a future refactor can't create
two live fraud jobs (two jobs → two fraud scores → "which is real?" has no answer).

### Worker job claiming — and a bug in the "obvious" idiom

The naive `SELECT ... FOR UPDATE SKIP LOCKED LIMIT 1` **acquires a lock but never marks the row** —
after commit the next poller grabs the same job. The correct idiom **locks and marks atomically**:

```sql
UPDATE processing.fraud_job SET status='PROCESSING', locked_by=:w, locked_at=now(),
       attempt_count = attempt_count + 1
 WHERE id = (SELECT id FROM processing.fraud_job WHERE status='PENDING'
             ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1)
RETURNING *;
```

### "Settle on terminal, not success" (resilience)

"OCR/analysis complete" means *every child job reached a **terminal** state* (succeeded **or**
retries-exhausted), not *every job succeeded*. So if 19 of 20 images finish and 1 is corrupt, the
stage settles once the bad one is dead-lettered; fraud proceeds on the 19 and the missing item becomes
a fraud *signal* — it never hangs forever.

### Incremental reprocessing

If an investigator requests a document, the customer uploads a **new version**; only that version is
re-OCR'd, the snapshot is rebuilt, and fraud re-evaluates — no full reprocess. A running fraud job is
never mutated; `pending_reprocess = TRUE` blocks the gate until it finishes, then re-drives.

### This is now built and tested (Phase 7)

The orchestrator, job tables, `SKIP LOCKED` claiming, the exactly-once gate, and a rule-based fraud
engine are implemented in Java (the Python OCR/analysis services are stubs that plug in behind the
same job interface). Concrete details worth citing:
- **`SKIP LOCKED` in Spring Data JPA:** `@Lock(PESSIMISTIC_WRITE)` + a query hint
  `jakarta.persistence.lock.timeout = -2` (Hibernate's SKIP_LOCKED) + `Pageable(1)` → `SELECT … FOR
  UPDATE SKIP LOCKED LIMIT 1`. N workers each claim a *different* job, never blocking.
- **The exactly-once gate is a `@Modifying` conditional JPQL UPDATE** on the single
  `claim_processing_state` row; if it returns `1`, insert one fraud job. A partial unique index
  (`WHERE status IN ('PENDING','PROCESSING')`) is the second layer. A test proves: no fraud job after
  OCR alone, exactly one after both stages, and idempotent on re-call.
- **The `@TenantId` lesson recurs here:** the fraud worker claims a job in one transaction, then binds
  `TenantContext` to the job's tenant **before** the processing transaction opens — because Hibernate
  resolves the tenant at session open, a mid-transaction change wouldn't scope the engine's claim/policy
  queries. Job/state tables are deliberately *not* `@TenantId` (workers must see all tenants' jobs);
  they carry a plain `tenant_id` the worker restores.

> Interview Q: *"Why not just use a message queue (Kafka/RabbitMQ)?"* — At V1 scale, a Postgres job
> table with `SKIP LOCKED` gives safe concurrent consumption, transactional enqueue (the job insert
> commits with the business change), and crash recovery, with zero extra infrastructure. A broker is
> the V2 move when throughput or fan-out demands it — and the job seam is already there.

---

## Part 9 — Validation philosophy: hard vs soft (MEDIUM)

At claim intake we split checks into two kinds:

- **Hard validations → reject immediately.** Certainly-invalid cases: policy not found, policy not
  active on the loss date, vehicle doesn't match the policy, claimant isn't the policyholder, incident
  in the future.
- **Soft validations → generate a fraud *signal*, never reject.** Cases that *might* be legitimate:
  claim amount > sum insured (the repair estimate often arrives later, or the amount was mistyped),
  repeat-claim history, amount anomalies.

**The principle:** reject only when you're *certain*; otherwise flag it and let a human decide. Rejecting
on "maybe" throws away legitimate claims. This mirrors how we treat EXIF and AI-image detection: a
missing signal is **UNKNOWN**, not **suspicious**.

> Interview Q: *"Give an example where being too strict backfires."* — Auto-rejecting a claim whose
> amount exceeds the sum insured, when the customer simply hadn't uploaded the final repair estimate
> yet. You'd deny a valid claim and generate a complaint. Flag it for the investigator instead.

**This is now built and tested** (`ClaimSubmissionValidator`). At claim submit: HARD checks throw and
return 400 (incident in the future, **policy not active on the loss date** — uses
`InsurancePolicy.isActiveOn`, claimant isn't the policyholder, **vehicle doesn't match** — compares on
the normalized registration); the SOFT check (amount > sum insured) returns a `warnings` list on a
**200** submission. The claim also **inherits the pinned product version from the policy** at intake,
so it will be adjudicated against the terms the customer agreed to. Tests prove all four paths.

---

## Part 10 — Problems we hit & how we solved them (WAR STORIES) ⭐

These are the "tell me about a bug / a hard problem" answers. Each is a real thing from this project.

**1. The app silently wouldn't boot — a Flyway migration named `V4_user_tables.sql`.**
Flyway requires a **double** underscore (`V4__…`) between version and description. With one underscore,
Flyway's parser fails to recognize it and **silently skips** the file (the "validate naming" option
defaults to off — no error, no log). So the `department`/`app_user` tables were never created, and
`ddl-auto=validate` failed at startup. *Fix:* rename to `V4__user_tables.sql` and set
`spring.flyway.validate-migration-naming=true` so a bad name fails loudly forever after.
*Lesson:* silent-skip defaults are dangerous; make them loud.

**2. A leftover database ahead of the code.** The dev DB had 20 migrations applied from an earlier,
discarded build, while the working tree had only 4. Flyway would refuse to start ("applied migration
not resolved locally"). Before touching anything, we `pg_dump`-ed the schema as a reference, confirmed
with the user it was intentional, then dropped/recreated the DB so the clean migrations applied.
*Lesson:* when the environment contradicts the code, investigate and preserve before you overwrite.

**3. A PUT that did nothing.** `updateInsuranceCompany` loaded the entity and saved it **without ever
applying the request** — the mapper's `updateEntity()` existed but was never called. PUT returned 200
with unchanged data. *Fix:* one line. *Lesson:* "returns 200" ≠ "did the thing" — verify effects, not
status codes. (We wrote a test that creates → updates → re-reads and asserts the value changed.)

**4. `@NotBlank` on an enum.** `UpdateInsuranceCompanyRequest` had `@NotBlank` on a `SubscriptionPlan`
enum. `@NotBlank` only works on `CharSequence`; on any other type it throws at validation time → every
PUT 500-ed. *Fix:* `@NotNull` for the enum.

**5. A committed database password.** `application.properties` (with a plaintext password) was tracked
in git, and `.gitignore` "ignored" it — but gitignore has **no effect on already-tracked files**, so
the rule was pure theatre and the secret sat in git history. *Fix:* externalize to `${DATABASE_PASSWORD}`
env var (no default → fail fast), fix `.gitignore`, and rotate the secret (it's burned once committed).

**6. Spring Boot 4 moved/renamed things (see Part 11).** `HibernatePropertiesCustomizer`,
`@AutoConfigureMockMvc`, and `TestRestTemplate` all changed. Cost us three compile/dep detours.

**7. No injectable `ObjectMapper` bean.** Our auth error handlers `@Autowired`-ed an `ObjectMapper`,
but Boot 4's modular setup didn't expose one as a bean → context failed to load. *Fix:* the error
bodies are tiny and fixed, so we write the JSON string directly and drop the dependency entirely.
*Lesson:* don't take a dependency you don't need for three fields of JSON.

**8. Authorization denials returned 500 instead of 403.** The global `@ExceptionHandler(Exception.class)`
catch-all intercepted Spring Security's `AccessDeniedException` (thrown by `@PreAuthorize`) **before**
the framework could translate it to 403. *Fix:* add an explicit `@ExceptionHandler(AccessDeniedException)`
→ 403 (a specific handler wins over the catch-all). *Lesson:* a catch-all exception handler will
swallow framework exceptions you actually want handled specifically — order and specificity matter.

**9. `@TenantId` + the create path.** Services used to set `tenant_id` manually from the path
variable. With `@TenantId`, Hibernate sets it from the resolver on insert. We removed the manual set so
there's exactly **one** mechanism — otherwise a mismatch between the path value and the token tenant
gets confusing. A test proves create now stamps the authenticated tenant automatically.

**10. Setting the tenant mid-transaction silently did nothing (refresh-token 401).** During token
refresh we validated the session, then `TenantContext.set(session.getTenantId())`, then loaded the
user with a normal `@TenantId` query — and it returned **404/401 every time**. Root cause: **Hibernate
resolves the current tenant *once, when the persistence Session opens*, not per query.** A refresh
request has no JWT, so the Session opened with the tenant unset (`SYSTEM_TENANT = -1`); setting
`TenantContext` *after* the Session was already open had **no effect** on subsequent queries, so the
user lookup ran against tenant `-1` and found nothing. *Fix:* refresh (like login) is a
pre-authentication, cross-tenant operation, so the user must be loaded via a **native query that
bypasses `@TenantId`** — not a mid-transaction context change. *Lesson:* `@TenantId` (discriminator
multi-tenancy) binds the tenant at Session/transaction start; you can't re-scope an open transaction by
mutating the ThreadLocal. Pre-auth lookups (login, refresh) inherently cross tenants and must use
native SQL.

**11. The customer portal was unreachable through our own API.** The portal scopes every read by
`app_user.customer_id` — that link is what makes it work. But `CreateUserRequest` had no `customerId`
field and `UserServiceImpl.create()` never called `setCustomerId`. So a `CUSTOMER` account created
through the API had a **null** `customer_id`: the person signed in successfully and landed in a
permanently empty portal. Every working portal login in the system existed only because it had been
**hand-written into SQL** by the demo seed — which is exactly why nobody noticed. *Fix:* `customerId`
on the request, **required** when `roleCode = CUSTOMER` and **rejected** otherwise (staff don't own a
customer), validated with a tenant-scoped lookup so you can't link across tenants.
*Lesson:* **a seed that bypasses your own API hides the fact that the API can't do the job.** When
fixture data is written in raw SQL, the product path it stands in for is untested by construction.

**12. `INVITED` was a status no account could ever leave.** Creating a user without a password set the
status to `INVITED`, and the DTO's own Javadoc said it was "to be used with Google sign-in on the same
email." But the auth code rejects anything that isn't `ACTIVE` — so an `INVITED` account could sign in
with **neither** a password (it had none) nor Google (`ACCOUNT_NOT_ACTIVE`). The documented feature had
never worked. In practice admins typed a password and told the person out of band, which means the
admin knows someone else's credential. *Fix:* a single-use, hashed, expiring invitation token emailed
as a "set your password" link — redeeming it is what promotes `INVITED → ACTIVE`. The same machinery
powers forgot-password. *Lesson:* **a state with no exit transition is a bug, not a state** — and a
comment claiming a feature works is not evidence that it does.

**13. Two subtle things the invitation flow got right (and why).** Redemption is *anonymous*, so
loading the user with a normal repository call would have repeated war story #10 — a `@TenantId` query
under `SYSTEM_TENANT` matching nothing. Both the read **and the credential update** go through native
SQL that bypasses tenancy. And `forgot-password` returns `204` whether or not the address exists:
responding differently would turn it into an **account-enumeration oracle**, letting anyone test which
emails have accounts. A test pins that both responses are identical.

---

## Part 11 — Spring Boot 4 / Hibernate 7 gotchas (MEDIUM–HARD)

Boot 4 **modularized** the autoconfigure jars and moved classes. Concrete changes we hit:

| Thing | Boot 3 | **Boot 4** |
|---|---|---|
| `HibernatePropertiesCustomizer` | `…boot.autoconfigure.orm.jpa` | `…boot.hibernate.autoconfigure` |
| `@AutoConfigureMockMvc` | `…boot.test.autoconfigure.web.servlet` | `…boot.webmvc.test.autoconfigure` |
| Integration HTTP client | `TestRestTemplate` | **dropped** — use `MockMvc` or the new `RestTestClient` |
| Web starter | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| Test starters | one `spring-boot-starter-test` | modular (`…-webmvc-test`, `…-data-jpa-test`, …) |

- **Hibernate 7.2** provides `@TenantId` and a *generic* `CurrentTenantIdentifierResolver<Long>` — our
  whole tenancy design depends on this, so we verified the version resolves before relying on it.
- We test with **MockMvc** (`@AutoConfigureMockMvc`), which runs the **full Spring Security filter
  chain** — so JWT parsing, the tenant filter, and `@PreAuthorize` are all exercised, not mocked.

> Interview Q: *"How do you handle a major framework upgrade?"* — Pin and verify the transitive
> versions you depend on (we confirmed Hibernate 7.2 for `@TenantId`), let compilation surface moved
> classes, and lean on integration tests that run the real filter chain to catch behavioral changes.

---

## Part 12 — Testing strategy (MEDIUM)

- **Integration-first for the foundation.** The things that matter (tenant isolation, auth, migrations)
  are only meaningful end-to-end, so we test through the real HTTP surface against a **real Postgres**.
- **No H2 substitute** — we use `JSONB`, partial indexes, `TIMESTAMPTZ`, and `SKIP LOCKED`, none of
  which H2 emulates faithfully. Testcontainers is the intended CI path; locally (no Docker) we point
  tests at a dedicated `claimlens_test` DB.
- **Seed via raw JDBC, read via HTTP.** For the isolation test this is deliberate: seeding through the
  filter would only prove the writer is filtered; seeding underneath it proves the **reader** is.
- **The migration test is a safety net:** if the context loads, Flyway applied every migration on a
  fresh DB *and* Hibernate `validate` matched every entity to the schema — exactly the check that would
  have caught the V4 naming bug on the first run.
- Current suite: **10 tests** — 5 tenant-isolation, 3 RBAC, 1 migration-validates, 1 context-loads.

---

## Part 13 — Rapid-fire Q&A (EASY → HARD)

**E: What is a tenant here?** An insurance company; each is isolated within a shared DB via `tenant_id`.

**E: Why background OCR/fraud instead of doing it in the request?** The customer shouldn't wait
seconds/minutes; submission returns immediately and processing happens async.

**E: Where do documents live?** Object storage (S3/R2); the DB stores only metadata + a pointer.

**M: How is tenant isolation enforced so it can't be forgotten?** Hibernate `@TenantId` on a mapped
superclass — applied to every query including `findById`, set automatically on insert.

**M: Why `roleId` in the JWT and not permissions?** Immediate revocation + small tokens; permissions
are resolved server-side from `role_permission`.

**M: Why 404 (not 403) for another tenant's record?** 403 leaks existence; 404 doesn't.

**M: How do you guarantee exactly one fraud job per claim?** A conditional atomic `UPDATE` on the
single `claim_processing_state` row (row lock serializes the racing completion handlers) + a partial
unique index as a second layer.

**H: What breaks tenant isolation, and how do you defend?** Native SQL bypasses `@TenantId`; we ban it
on tenant-scoped entities (test-enforced) and can add Postgres RLS later.

**H: A worker crashes mid-job — how do you not lose or double-run it?** Jobs are claimed by atomically
flipping `PENDING → PROCESSING` with `locked_at`; a recovery sweep resets stale `PROCESSING` rows past
a threshold back to `PENDING` (or `DEAD_LETTER` after max attempts). At-least-once ⇒ consumers are
idempotent.

**H: Customer uploads a new document mid-investigation — do you reprocess everything?** No. Only the new
document version is OCR'd; the claim snapshot is rebuilt and fraud re-evaluates. A running fraud job is
never mutated (`pending_reprocess` blocks the gate until it completes).

**H: Why version-pin a claim to the policy's product version, not the product's current version?**
Because the customer is covered by the terms they bought; using the current version would adjudicate
against terms they never agreed to — invisible in testing until a second version ships.

---

## Part 14 — Trade-offs & things we'd revisit (HARD / honesty points)

Interviewers respect candidates who know their design's limits.

- **Shared-schema multi-tenancy** trades isolation for cost/simplicity; a single bad native query could
  cross tenants. Mitigation: `@TenantId` everywhere + (future) RLS.
- **`sum_insured` over-limit check is a V1 simplification** — it should subtract prior approved claims in
  the period (it's an *aggregate annual* limit). Shipped as a soft signal and documented as such.
- **AI-image / synthetic detection is best-effort** — no detector is reliable; it's always a soft signal
  for the investigator, never an auto-reject. V1 uses cheap, high-precision-when-present checks (C2PA
  provenance) and defers the ML detector.
- **Deferred for later phases:** the login/refresh auth module, the `BaseEntity` soft-delete split,
  and the whole AI/RAG layer (sequenced last — it needs product documents). *(Redis permission
  caching was deferred at Phase 2 and has since shipped — see "Caching" below.)*

---

## Appendix — Build/run cheat-sheet

```bash
# Run the app locally (needs both env vars; secrets are never committed)
DATABASE_PASSWORD=<pw> JWT_SECRET=<32+ byte string> ./mvnw spring-boot:run

# Run the test suite (against the local claimlens_test DB)
DATABASE_PASSWORD=<pw> ./mvnw test
```

**Progress so far:** Phase 0 (unblock) ✅ · Phase 2 (tenancy + security + auditing + RBAC) ✅ ·
Phase 3a (login/refresh auth module) ✅ · Phase 3b (customer) ✅ · Phase 4 (product + version +
insurance policy + vehicle, **version pinning proven**) ✅ · Phase 6a (**claim intake**) ✅ · Thin slice
(upload → assign → decide) ✅ · Phase 7 (**real async pipeline**: OCR/analysis workers with
`SKIP LOCKED`, the exactly-one-fraud-job gate, rule-based fraud scoring) ✅ · Phase 5 (**config-driven
fraud rules**: `fraud_ruleset`/`fraud_rule` + evaluator registry — admins weight/toggle named checks
without code) ✅ · Phase 9 (**assignment engine**: auto-assign the least-loaded eligible investigator,
alongside manual assign; **+ investigation notes** — site visits / fraud observations / customer
interactions with severity and an optional evidence link) ✅ · Phase 10 (**audit + notification +
analytics**) ✅ — **35 tests green**.

**Audit ("auditability by default") is aspect-driven:** an `@Auditable(action, entityType)` annotation
on a service method + an AOP `@AfterReturning` aspect that records the action, the acting user (from
the security context), the tenant, and the entity id — automatically, only on success (a method that
throws writes no audit). The audit table is **append-only** (no `updated_at`, no soft-delete — an
audit log you can edit isn't one), and the record joins the business transaction so a rolled-back
action leaves no trace. Notifications are in-app (email deferred behind the same service); the
dashboard is **live aggregate queries** (`GROUP BY` on claim status / fraud risk), no pre-computed
snapshots at V1 scale.

> Interview Q: *"Why an aspect instead of calling an audit service explicitly everywhere?"* — Explicit
> calls get forgotten and scatter cross-cutting concern through business logic; an annotation +
> aspect makes auditing declarative and impossible to forget on annotated actions. The trade-off is a
> little AOP indirection, which is worth it for a compliance guarantee. Full claim flow:
`DRAFT → AWAITING_ANALYSIS → [OCR + analysis] → [fraud] → AWAITING_ASSIGNMENT → UNDER_INVESTIGATION → APPROVED/REJECTED`.

**Config-driven fraud (the ruleset engine):** each fraud check is a `FraudRuleEvaluator` bean
(`triggers(claim, policy)`); a `fraud_rule` row decides whether it's enabled and its weight; the
engine sums the enabled rules' weights and bands the total against the ruleset's thresholds, falling
back to built-in defaults when unconfigured. Adding a check = one bean + one config row, no engine
change. Rules are *data*; the menu of possible checks is *code*. ("ruleset", not "policy" — the word
policy is reserved for the insurance contract.) The whole V1 happy path is demoable except OCR/analysis/fraud, which are stubbed
in the manual path. Deferred: the Python OCR/analysis workers + orchestrator + fraud engine, ruleset
engine, document versioning, notifications/analytics/audit, frontend, deployment.

**Document storage is behind an interface** (`DocumentStorage`): the DB only ever stores a
`storage_key` pointer; a local-filesystem implementation is the dev default, and an S3/R2
implementation would be a drop-in swap. **A schema gotcha worth remembering:** an entity that extends
the tenant-aware base inherits the audit + soft-delete columns, so its table must have them or
Hibernate `validate` fails at startup — infrastructure tables (assignment, session, status-history)
are therefore standalone entities with `@TenantId` (or a plain `tenant_id`) instead.

**Why authentication seeding in tests uses raw JDBC, not JPA** (a likely question given the code):
the tenant-isolation test must insert rows for tenant B *without* going through `@TenantId` (which
would stamp the wrong tenant), and must be independent of the code under test (so a bug in the write
path can't mask a bug in isolation). Rule: **seed beneath the abstraction, read through it.**

**Phase 11 — the frontend** ✅ (`next build` clean, 20 routes). Next.js 16 App Router + TypeScript
strict + Tailwind v4 + shadcn/ui (Radix) + Redux Toolkit + a single axios instance + react-hook-form
+ dayjs + next-themes. The load-bearing idea: **it is one app, not six.** The vision's "six portals"
(admin, product manager, claims/investigation manager, investigator, customer support,
auditor/analyst) are realised as **permission-adaptive navigation** — the sidebar, the landing route,
and every action button are gated by the user's permission codes, so each role sees exactly its own
product without a separate build.

> Interview Q: *"How does the UI know a user's permissions?"* — **Not from the JWT.** The token carries
> only `roleId` (so revocation is immediate server-side); the UI calls **`GET /auth/me`** on login,
> which resolves the permission codes, and gates rendering on those (`<Can permission=…>`). Mirrors
> exactly the `@PreAuthorize` codes the backend enforces — one source of truth, added as a small new
> endpoint for the frontend.

> Interview Q: *"Why axios + a tiny `useAsync` hook instead of RTK Query?"* — a deliberate
> simplicity trade-off: one axios instance owns the cross-cutting concerns (Bearer token,
> **single-flight refresh-on-401-then-retry**, error normalisation) in interceptors; server state
> that must live app-wide (the session) goes through Redux; everything else is a `useAsync`/
> `useMutation` pair. Less machinery to reason about at this scale; the cost is manual cache
> invalidation via `refetch()`.

**A tooling war story (shadcn's two base libraries):** shadcn's current CLI defaults to a **Base UI**
preset whose components use a `render` prop, not Radix's `asChild`. Code written to the classic
`asChild` convention fails to typecheck against it. Fix: re-init with `-b radix` so components use
Radix `Slot`. Lesson — when a generator has multiple component-library backends, pin the one your
composition patterns assume *before* writing against it.

**The OCR service (`ocr-service/`)** — a small **stateless FastAPI** service that turns a claim
document (image or PDF) into text plus best-effort structured fields (registration / policy / chassis /
engine numbers, dates, amounts via regex). Two design decisions worth defending:

> Interview Q: *"Why is the OCR service stateless with no database, when it's part of the pipeline?"* —
> The backend already owns the hard parts: tenant scoping, transactional job-claiming, the
> exactly-once fraud gate. If the Python service also touched the DB it would duplicate that logic and
> couple two codebases to one schema. So the backend **claims the OCR job, sends the document over
> HTTP, and persists the result**; the service is a pure function `bytes → text+fields`. That also
> lets it scale and deploy independently, and be swapped for a managed OCR API without backend changes.

> Interview Q: *"How do you swap Tesseract for Google Vision?"* — Both implement one `OcrEngine`
> interface, selected by an `OCR_ENGINE` env var. Tesseract is the free, offline, local-dev default;
> Vision is the higher-accuracy engine for the deployed demo. The seam is fixed so the choice is a
> config change, never a rewrite — same principle as the storage interface.

**Two integration safeguards worth calling out.** (1) The backend's `OcrClient` has a **`NoOpOcrClient`
default** and an `HttpOcrClient` that only activates on `claimlens.ocr.enabled=true` — so tests and
un-wired environments run the exact same pipeline with OCR simply producing no text, and the 38-test
suite stays green while real OCR is fully optional. (2) `HttpOcrClient` **never throws**: a down or
slow OCR service returns an empty result, and because the fraud gate settles on *terminal* not
*success*, the claim still progresses (the missing text becomes an investigator signal, never a hang).
The OCR worker fetches the claim's documents with a **native query that bypasses `@TenantId`** — the
same trick as the auth lookups, because a background worker has no request tenant context.

**The analysis service (`analysis-service/`)** — the OCR service's sibling, and the product's fraud
"intelligence." Same shape (stateless FastAPI, engine-swappable), but it produces **image fraud
signals, never decisions**. Four signals, all lightweight CV (no deep learning):

- **Duplicate detection** via perceptual hashing (`imagehash` pHash/dHash). The top motor-fraud pattern
  is reusing one damage photo across claims. Key design point: the service is stateless, so it just
  *returns* the hash; the **backend** stores hashes and flags reuse when a new photo's pHash already
  exists on a *different* claim. That comparison belongs where the data lives.
- **Similarity** via **ORB** feature matching (OpenCV) on `/compare` — catches lightly-edited reuse
  (crop/rotate/recolour) that a hash alone misses. ORB not SIFT: faster and free.
- **EXIF, three-state** — the interview-favourite edge case. `CONSISTENT / INCONSISTENT / UNKNOWN`, and
  **only INCONSISTENT raises the score**. *Missing EXIF is UNKNOWN, never suspicious* — WhatsApp and
  every social app strip EXIF, so absence is the norm; penalising it would flag most honest claims.
  INCONSISTENT means something concrete: a capture time that predates the incident, or GPS far from the
  reported location.
- **Synthetic/AI image** — deliberately a **soft signal only**, because no reliable detector of clean
  AI generation exists. ELA recompression heuristic + C2PA provenance-marker presence, weighted low,
  never an auto-reject. A real CNN/commercial detector is the V2 swap behind the same function.

> Interview Q: *"Why is 'missing EXIF' not a fraud signal?"* — Because it's the common case for
> legitimate claims (photos arrive via WhatsApp, which strips metadata), so treating absence as
> suspicion would bury investigators in false positives. The three-state model encodes that: we only
> escalate on *contradiction*, not on *absence*. Same philosophy as hard-vs-soft validation and the
> "settle on terminal, not success" gate — never punish the claimant for a data gap.

Both Python services share the backend integration pattern: a `NoOp` client by default (tests and
un-wired envs run unchanged, all **38 backend tests stay green**), a conditional `Http` client that
activates per config, a result table (`ocr_result`, `analysis_result`), and a worker that reads
documents via the tenant-bypassing native query. Real intelligence, zero risk to the existing suite.

**Phase 12 — policy intelligence (RAG)** ✅. Ingest a product version's policy wording → chunk → embed
→ store; then answer coverage questions ("Is windshield damage covered?") by retrieving the most
similar chunks and asking an LLM, **with citations back to the exact excerpts**. Surfaced as a
"Coverage" tab on the claim (asks about that claim's policy) and a "Knowledge" ingest dialog per
product version. Several decisions defended:

> Interview Q: *"You used RAG without a vector database?"* — pgvector wasn't available on the target
> Postgres (and `CREATE EXTENSION vector` needs elevated RDS privileges — the plan said to verify
> that *before* building on it, and it failed). So embeddings are stored as JSON and cosine-ranked
> in-process. At V1 scale — hundreds of chunks per product version — an O(n) scan per query is
> imperceptible, and it's completely portable. The seam is clean: swap to pgvector + HNSW for ANN
> scaling later (Neon supports it) without touching the API. Don't add a database dependency you
> can't deploy.

> Interview Q: *"You deployed on Neon, which DOES support pgvector — so did you use it?"* — Yes, as a
> **gated, swappable retrieval strategy** — the honest answer is "both, chosen by config." Retrieval
> lives behind a `CoverageVectorSearch` interface with two impls: `CosineVectorSearch` (default —
> JSON-text + in-Java cosine, runs on *any* Postgres, keeps all tests green with no extension) and
> `PgVectorSearch` (active when `PGVECTOR_ENABLED=true`, i.e. on Neon — a `vector(768)` column with an
> **HNSW** index, cosine distance `<=>` offloaded to the database). A gated `PgVectorInitializer`
> `ApplicationRunner` runs the idempotent DDL (`CREATE EXTENSION vector`, `ADD COLUMN`, `CREATE INDEX …
> USING hnsw`) on startup — deliberately *outside* Flyway so the local/test DBs (no pgvector) never
> try to create it. Two subtleties worth naming: HNSW (not ivfflat) so the index builds fine on an
> empty table and needs no rebuild; and the pgvector query is **native SQL, which bypasses `@TenantId`**,
> so the tenant filter is re-applied explicitly in the WHERE clause — the same D7 leak guard, by hand.
> Net: no local install, tests unaffected, and the deployed demo runs a real ANN vector index.

> Interview Q: *"How does it run offline, with no API key, in tests?"* — Same NoOp-by-default pattern:
> the default `EmbeddingClient` is a **signed feature-hashing vectoriser** (like a tiny
> HashingVectorizer) — deterministic, no network, and crucially cosine similarity between two of its
> vectors tracks *shared vocabulary*, so a question about "windshield" genuinely retrieves the
> windshield chunk. The default `ChatClient` is extractive (returns the top excerpts). Flip
> `claimlens.ai.enabled=true` and it's Gemini embeddings + `gemini-1.5-flash`, behind the same
> interfaces. The whole RAG flow — including the integration test that asserts the windshield chunk
> ranks top — runs with zero external dependencies.

> Interview Q: *"Tenant isolation on the AI layer — the scary one?"* — This is exactly the leak the
> plan (D7) warned about: if the chunk table lacked `tenant_id`, the filter would depend on a join
> back through the product, and one forgotten join is a **cross-tenant AI answer leak**. Every RAG
> table is mapped `@TenantId`, so retrieval is auto-scoped by Hibernate — a tenant physically cannot
> retrieve another's chunks, proven by a test where tenant B asking about tenant A's version gets a
> 404, not an answer. Retrieval is *also* filtered to the claim's **pinned** product version, so the
> answer reflects the terms the customer actually contracted under.

That keeps the suite at **44 backend tests green**, all four services (backend, OCR, analysis, RAG)
integrated and visible in the UI.

---

## Nice-to-haves — lifecycle completeness & the email channel (57 backend tests green)

After the core V1 shipped, a round of gap-closing that a reviewer notices:

**User CRUD, product edit/retire, policy cancel.** V1 could create these aggregates but not amend
them — a visible hole. Added `POST/PUT /users`, `PUT /products/{id}` (rename + status incl.
`RETIRED`), and `POST /policies/{id}/cancel`. All gated on the existing write permissions at the
**service** layer (`USER_WRITE`, `PRODUCT_WRITE`, `POLICY_WRITE`) — no new auth surface, and tenant
isolation is inherited from `@TenantId`, so the cancel/update of another tenant's row is a 404 by
construction, not a hand-written check.

> Interview Q: *"Why is 'cancel policy' a POST to a sub-path, not a PUT with status in the body?"* —
> Cancellation is a **state transition with an invariant** (a cancelled policy can't be un-cancelled,
> and later it will need to reject in-flight claims), not a field edit. Modelling it as
> `POST /policies/{id}/cancel` keeps the transition explicit and gives it a single audit point,
> versus a generic PUT where any caller can set any status. Same reason product *activation* is a
> POST to `/versions/{id}/activate`.

**Email as a second delivery channel — swappable sender.** In-app `NotificationService` gained a
best-effort email copy behind an `EmailSender` interface — the same conditional-client pattern used
for storage, OCR, analysis and AI:

- `LogEmailSender` — **default** (`claimlens.email.provider` unset or `log`, `matchIfMissing=true`):
  logs the message. Tests, CI and the demo run with no mail server.
- `ResendEmailSender` — `havingValue=resend`: real delivery via the Resend HTTP API (`RestClient`,
  Bearer key). The deployed sender, from a verified domain. API key is a secret (env only).
- `SmtpEmailSender` — `havingValue=smtp`: real delivery via Spring `JavaMailSender` (`spring.mail.*`).

Three providers, one interface — the same swappable-client shape as storage/OCR/AI. Adding Resend was
a new `@ConditionalOnProperty` bean and three config lines; nothing else changed. The API path (not
SMTP) was chosen for Resend because an API key was the credential given, and HTTP avoids blocked
SMTP ports on locked-down PaaS.

> Interview Q: *"What happens if the SMTP server is down when a claim is assigned?"* — Nothing
> visible. The email dispatch resolves the recipient (a `@TenantId`-scoped lookup, so it can only
> ever reach a same-tenant user) and is wrapped so **any failure is logged and swallowed** — the
> in-app notification is already persisted, and the assignment transaction that triggered it must not
> roll back because a mail server hiccuped. Notification delivery is best-effort by design; the
> durable record is the in-app row. A unit test asserts exactly this: `emailSender.send` throwing
> still persists the notification.

> Interview Q: *"Why add the mail starter if it's off by default?"* — The starter only puts
> `JavaMailSender` on the classpath; Spring's mail auto-config stays dormant without `spring.mail.*`,
> and `SmtpEmailSender` is `@ConditionalOnProperty` so it isn't even instantiated in the default
> profile. Cost is one dormant bean definition; benefit is that flipping `EMAIL_PROVIDER=smtp` in
> prod needs zero code change — the seam is built, not bolted on later.

### Rich claim reports — branded HTML + a PDF attachment

A plain-text email is a poor artifact for an insurance decision, so every claim email is now a
**branded HTML message with an attached "Claim Report" PDF** — key stats, the decision and who made
it, the investigator, the fraud level, and the customer's **uploaded photos**.

- **The `EmailSender` interface grew a `send(EmailMessage)`** carrying an HTML body, a plain-text
  fallback, and attachments. It's a **default method that degrades to the plain-text primitive**, so
  the three senders only override it if they can do more: `ResendEmailSender` posts `html` + base64
  `attachments`; `SmtpEmailSender` builds a MIME multipart; `LogEmailSender` logs a summary. No sender
  was forced to change — the old text path still works.
- **Rendering is split by medium, deliberately.** The **PDF embeds image bytes** (fetched from
  `DocumentStorage`, served to openhtmltopdf under a private `claimimg://` scheme) so it shows the
  photos on *any* storage backend, including local dev. The **HTML email uses public R2 URLs** (added
  `DocumentStorage.publicUrl`) because inline images in email are unreliable (Gmail strips data URIs) —
  and it gracefully omits thumbnails when no public URL exists. The customer's main ask — "the report
  with the photos" — is always satisfied by the PDF; the email additionally shows them in prod.
- **openhtmltopdf, not hand-laid-out boxes.** The report is XHTML+CSS rendered to PDF, so it shares a
  visual language with the email instead of imperative PDF drawing. One gotcha handled: the built-in
  Helvetica has no ₹ glyph, so the PDF uses `Rs` while the HTML email keeps ₹ and emoji.
- **The renderers are pure functions** (`ClaimReportRenderer` — no I/O, no Spring), so a plain unit
  test asserts the HTML carries the claim number/status/names and the PDF is a valid `%PDF-` document
  with an image embedded — without spinning a context.
- **Best-effort, end to end.** PDF generation is wrapped so a render failure still sends the HTML
  email; the whole email dispatch is still swallowed on failure so it can't roll back the claim
  transaction. And it's **gated off in tests** (`claimlens.email.rich.enabled=false`) so the suite
  exercises the plain path and never renders a PDF per decision.

> Interview Q: *"Isn't generating a PDF on the request thread slow?"* — Yes, it's the honest cost
> here: image downloads + render run inside the claim transaction (a few hundred ms). Acceptable for a
> demo, and wrapped so it never breaks the claim. The clean fix — which I'd reach for under real load —
> is a `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` so the report is built and sent *after*
> the claim commits, off the request thread. The seam (a context object handed to a report service) is
> already shaped for it.

### The information-request loop — a two-way conversation, closed end-to-end

An investigator often needs one more document. The full round-trip is wired:

1. **Investigator asks** — `POST /claims/{id}/request-information {message}` (`CLAIM_INVESTIGATE`).
   The claim goes `UNDER_INVESTIGATION → WAITING_FOR_CUSTOMER`, and the policyholder gets the rich
   INFO_REQUESTED email with the message.
2. **Customer answers** — they upload the document from the portal
   (`PortalService.uploadToMyClaim`). That calls `claimService.recordCustomerResponseInternal`,
   which — **only if the claim was WAITING_FOR_CUSTOMER** — flips it back to `UNDER_INVESTIGATION`,
   re-runs the pipeline, and notifies the assigned investigator (CUSTOMER_RESPONDED email).
3. **Reprocess + re-decide** — `ProcessingOrchestrator.onCustomerResponse` resets the per-claim
   processing state and re-queues OCR + analysis; fraud re-evaluates through the same exactly-once
   gate, and the investigator decides on the fresh picture.

Design points worth stating:
- **The trigger is a guarded no-op, not a new endpoint.** Any portal upload calls the same method;
  it does nothing unless the claim is actually waiting. So a customer adding a document to a *draft*
  during initial filing doesn't accidentally "re-open" anything — the status guard is the whole
  control logic, and there's exactly one upload path to reason about.
- **`recordCustomerResponseInternal` is an internal method with no `@PreAuthorize`** — the portal
  already enforced ownership (customer owns the claim). Same internal-composition pattern as
  `createDraftInternal`/`submitInternal`: staff permissions never leak to a customer path.
- **Honest limitation, stated in the roadmap.** OCR/analysis workers are *claim-scoped* (one job
  processes all the claim's documents), so a response re-OCRs the **whole** claim, not just the new
  file. True per-document reprocessing (per-doc OCR jobs, a derived claim status) is the next
  refinement — the loop is correct today, just not yet minimal. Re-running the pipeline is safe
  because `fraud_score` is append-only (latest-wins on read), which an integration test verifies by
  driving the pipeline twice.

### Document versioning — completing the deferred Phase-6 slice (59 backend tests green)

V1 shipped a **single-version** `document` table on purpose (a thin slice to get claims clickable
earlier). Versioning fills the gap without forking the pipeline:

- `document` is now the **logical** document; each upload is a `document_version` row (immutable once
  written). `document.current_version_id` points at the live version, and the `document`'s own
  file_name/storage_key/etc. stay as a **mirror of the current version** — so OCR (which reads
  `document.storage_key` via a native query) and the existing download endpoint keep working with
  **zero changes**.
- New surface: `POST /claims/{c}/documents/{d}/versions` (revise), `GET .../versions` (history),
  `GET .../versions/{v}/download` (fetch any past revision).

> Interview Q: *"Why keep the mirror fields on `document` instead of always joining to the current
> version?"* — Backward compatibility with a live pipeline. The OCR worker and the fraud snapshot
> already read `document.storage_key`; making versioning a pure join would have forced a change to
> every reader (including a **native** OCR query that bypasses `@TenantId`). Mirroring the current
> version onto the document keeps those readers untouched and correct — a re-upload just repoints the
> pointer and refreshes the mirror in one transaction. The `document_version` rows are the durable
> history; the mirror is a denormalized convenience, same pattern as the claim's `policy_number`
> snapshot.

> Interview Q: *"What about documents that existed before the migration?"* — The `current_version_id`
> column is nullable and there are no version rows for legacy documents. Rather than a data-migration
> backfill (there's no prod data yet, but the code shouldn't assume that), the service
> **lazily materializes a v1** from the document's snapshot the first time it's revised, so version
> numbering stays contiguous (old → v1, new → v2). Adding the column to an empty table is free;
> lazy-backfilling means the feature is correct even against pre-existing rows.

> Interview Q: *"Tenant isolation on a nested sub-resource?"* — Same `@TenantId` guarantee. Every
> version operation first loads the parent `document` with a tenant-scoped lookup (cross-tenant id →
> 404), then fetches the version constrained to that `document_id`. A test proves tenant A gets a 404
> revising tenant B's document, and a second proves the original bytes survive a re-upload (download
> v1 → original bytes, default download → the corrected bytes).

**Deferred deliberately:** per-version OCR *delta* reprocessing (re-run OCR only on the changed
version, driven from an investigator's information request) is the heavier processing-pipeline half
of the plan's "incremental resubmission." Because the mirror always reflects the newest bytes, the
next processing run already picks up the revision — so the versioning capability is complete and
demoable; the delta-only optimization is a follow-up, not a prerequisite.

### Customer self-service portal — a second security boundary below the tenant (63 backend tests green)

V1 was a B2B *staff* tool: claims were raised by staff on behalf of a policyholder. The portal lets
the **policyholder log in and file + track their own claim**. The headline is not the UI — it's the
new security boundary.

> Interview Q: *"You already have `@TenantId`. Why is a customer portal a security problem at all?"*
> — Because `@TenantId` scopes to the **tenant**, and two customers share a tenant. Customer A and
> customer B are both inside "Demo Insurance", so the discriminator does nothing to separate them. A
> customer portal needs a **second boundary, below the tenant** — ownership. That's the whole risk: a
> single missing `WHERE customer_id = :me` and customer A reads customer B's claim, and the tenant
> filter looks like it's protecting you the entire time.

> Interview Q: *"So how do you enforce ownership without it leaking?"* — Three deliberate choices:
> 1. **A disjoint permission set.** The `CUSTOMER` role holds only `PORTAL_*` permissions and *none*
>    of `CLAIM_READ/WRITE/SUBMIT` or `POLICY_READ`. So a customer physically cannot call any staff
>    endpoint — those `@PreAuthorize` on the staff permissions a customer will never have. The portal
>    is the only surface they can reach.
> 2. **A single ownership gate.** Every portal read/write goes through
>    `findByIdAndCustomerIdAndIsDeletedFalse(claimId, currentCustomerId())` — the customer id comes
>    from the **JWT principal**, never the request body. A claim in the same tenant owned by someone
>    else simply isn't found → **404, not 403** (a 403 would confirm the id exists).
> 3. **Ownership is a gate test, like tenant isolation was.** `CustomerPortalIntegrationTest` proves
>    A gets 404 for B's claim in the same tenant, can't submit/upload to it, can't even file against
>    B's policy, and that a staff token (no `PORTAL_*`) is 403 on the portal. Nothing shipped until
>    that was green.

> Interview Q: *"The portal reuses the claim/document logic — did you duplicate it or grant customers
> the staff permissions?"* — Neither, because both are traps. Duplicating the version-pinning +
> validator + processing-orchestration logic invites drift; granting customers `CLAIM_WRITE` would
> let them hit the staff `POST /claims` and pass an arbitrary `customerId`. Instead I split each staff
> service method into a thin **authorized wrapper** (`@PreAuthorize('CLAIM_WRITE')`) and an
> unannotated **internal** method (`createDraftInternal`, `submitInternal`, `uploadInternal`, …). The
> `PortalService` — gated with `PORTAL_*` and having *already* checked ownership — calls the internal
> method. No controller maps to the internal methods, so they're not a public surface; the shared
> business logic has exactly one implementation.

> Interview Q: *"How does the customer id get from login to the ownership query, statelessly?"* — A
> new nullable `app_user.customer_id` links a login to a policyholder (NULL for every staff user). It
> rides in the JWT as a `cid` claim and onto `ClaimLensPrincipal`, so the portal resolves "who is
> this customer" with zero DB lookups — the same stateless-principal pattern as tenant and role. A
> staff login has `cid = null`, so if one ever reached a portal method the ownership resolver throws
> `NOT_A_CUSTOMER` — defense in depth behind the permission gate.

---

## Current state — what's actually built and proven (103 backend tests green)

The phase headings above carry the test count *at the time*, so they climb: 35 → 44 → 57 → 59 → 63.
Where it stands now:

| Area | State |
|---|---|
| Core V1 | Multi-tenancy, RBAC, claims lifecycle, fraud engine, RAG, customer portal, platform console |
| Performance | Permission cache (Caffeine dev / Redis prod), tuned HikariCP, gated pgvector + HNSW |
| Security | Auth rate limiting (per IP) + cost limiting on AI/uploads (per user), invitation & reset tokens |
| Lifecycle | Two-way information requests — request → customer responds → auto re-open + reprocess |
| Comms | Branded HTML emails + an attached PDF claim report with the photos embedded |
| Tests | **103** (3 skipped, all env-gated: a live-Vision check and two manual email senders) |

**Proven against real infrastructure, not just locally:** pgvector runs on Neon (extension, HNSW
index, ANN query, and a real ingest → ask round trip), the Redis cache runs on Upstash, and both
Gemini embeddings and chat return real answers with citations.

**Known gaps, stated honestly** — worth naming these before an interviewer finds them:
- **Observability** is the one Phase-2 item left: no Micrometer/Prometheus metrics, no structured
  JSON logs with trace/tenant/user MDC.
- **Presigned URLs** aren't built, so claim-photo thumbnails don't render inline in email (the PDF
  carries them instead). The alternative — a public bucket — would put claimants' licences and FIRs
  on the open internet, which isn't a trade worth making for a thumbnail.
- **Per-document reprocessing**: a customer response re-runs the whole claim's pipeline rather than
  just the changed file, because OCR/analysis jobs are claim-scoped.
- **PDF generation runs on the request thread** inside the claim transaction; the fix is
  `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`.

---

## Live end-to-end testing session — eight bugs the test suite never caught (103 backend tests green)

Everything below was found by **driving the running app**, not by reading code or running tests. That
is the lesson in itself: 92 green tests, a clean deploy, and the first ten minutes of clicking through
a real flow surfaced a critical cross-tenant write. **Tests prove the paths you thought of.**

### 1. The tenant root had no guard at all ⭐ (the best story here)

**Symptom.** Logged in as the tenant admin of *Demo* (tenant 2), issued
`PUT /organizations/companies/1` — *Acme*, a different tenant — and got `200 OK`. The rename
persisted. A tenant admin could read **and overwrite** every other tenant's company record.

**Why the architecture didn't stop it.** `InsuranceCompany` deliberately extends `BaseEntity`, not
`TenantAwareEntity`. That is correct: it is the tenant *root*, so a `@TenantId` discriminator would
make it unloadable before the tenant is known — chicken-and-egg at login. But that means **Hibernate's
tenant filter never applies to it**, and the design note said it must therefore sit behind a
platform-admin authority. It never did. The only guard was `ORG_COMPANY_READ` / `ORG_COMPANY_WRITE`,
and the seed grants those to `TENANT_ADMIN` and `AUDITOR` too:

```sql
SELECT r.code, p.code FROM role_permission rp
  JOIN role r ON r.id=rp.role_id JOIN permission p ON p.id=rp.permission_id
 WHERE p.code LIKE 'ORG_COMPANY%';
-- AUDITOR|ORG_COMPANY_READ   TENANT_ADMIN|ORG_COMPANY_READ   TENANT_ADMIN|ORG_COMPANY_WRITE
```

So the one aggregate the automatic mechanism *cannot* protect was left relying on it. **The exception
to a safety net is exactly where the manual check has to be, and exactly where it gets forgotten.**

**Fix.** Cross-tenant operations (list-all, create, delete) now require `PLATFORM_ADMIN`. Per-id
read/update call `assertOwnTenantOrPlatformAdmin(id)`, which throws **404, not 403** — a 403 confirms
the row exists and leaks other tenants. Added `GET /companies/me` so a tenant admin reaches their own
company without ever naming an id. The frontend had the same hole: the "Companies" nav item was gated
on `ORG_COMPANY_READ`, so the UI actively *offered* tenant admins a list of every tenant — re-gated to
`PLATFORM_ADMIN`.

**Interview framing:** *"Where is your authorization weakest?"* → wherever a global mechanism has a
deliberate exception. Enumerate the exceptions; each one needs an explicit, tested guard.

### 2. `/companies/me` returned 500 — and so did every bad id

The endpoint didn't exist, so `/companies/me` fell through to `/companies/{companyId}`, `"me"` failed
to bind to `Long`, and `MethodArgumentTypeMismatchException` escaped to the catch-all `Exception`
handler as `INTERNAL_SERVER_ERROR`. That affected **every** `{id}` route, not just companies: any
non-numeric id reported a server fault for what was a malformed request. Added an explicit handler
returning `400 INVALID_PATH_PARAMETER`. A catch-all that returns 500 hides client errors as server
errors — each one you can name deserves its own handler.

### 3. A silent AI fallback that lied about which model answered

The RAG returned a bullet-dumped extract starting mid-word — the *stub* format — while the response
still reported `model: "gemini-3-flash-preview"`. `GeminiChatClient` holds
`private final ChatClient fallback = new StubChatClient()` and degrades to it on any failure, but
`model()` kept returning the configured Gemini name. So a degraded answer was **indistinguishable from
a real one** — in the API response, in the persisted `coverage_answer` audit row, and in the UI.

Fix: `ChatClient.answer()` now returns `Answer(text, model)` carrying the model that *actually*
produced the text, and the fallback reports `stub-extractive (gemini-fallback)`. Also hardened the
response parsing — `String.valueOf(p.get("text"))` spliced the literal string `"null"` into answers
when a part carried no text, and reaching into `content.get("parts")` NPE'd when a candidate came back
with no parts at all.

**The principle:** graceful degradation is good; *silent* degradation is not. If a system can quietly
serve a worse answer, it must say so — otherwise you cannot tell a healthy system from a broken one.

### 4. My own "fix" made it worse — and the real quota was 100× smaller than assumed

Having seen a `503 UNAVAILABLE` ("model experiencing high demand"), I added a retry. Measured after:
degraded answers went from **1-in-8 to 8-in-12**. The retry was actively harmful, because the real
error was not 503:

```
429 RESOURCE_EXHAUSTED
quotaId: "GenerateRequestsPerDayPerProjectPerModel-FreeTier"
limit: 20, model: gemini-3-flash   —   "Please retry in 28.7s"
```

Two compounding lessons:

1. **Never retry a 429 on a short backoff.** The response stated its own retry delay (~28s); retrying
   after 400 ms cannot succeed *and* spends another request against the exhausted quota. Retry 5xx
   (genuinely transient); fail straight through on 429.
2. **Read the `quotaId`, not the number.** `limit: 20` looked like a per-minute cap. It is
   `RequestsPerDay` — `gemini-3-flash-preview` allows **20 chat requests per day** on the free tier. A
   single recruiter browsing the demo would exhaust it in minutes. Verified empirically across models
   with the same key: `gemini-2.5-flash` → 200 OK; `gemini-3-flash-preview` → 429 (20/day);
   `gemini-2.0-flash` → 429 with **limit: 0** (no free quota at all). Default switched to
   `gemini-2.5-flash`; after the switch, 6/6 questions returned real cited answers.

**Interview framing:** *"Tell me about a time you made something worse."* — I diagnosed from the first
error I saw instead of the error that was actually happening, and I shipped a retry without measuring
it. The measurement is what caught it.

### What the RAG actually does, verified live

Ingested an 8-section motor policy wording → 7 chunks, real `gemini-embedding-001` vectors. Then:

| Question | Answer | Correct source |
|---|---|---|
| NCB after 3 claim-free years | 35 percent | §6 ✅ |
| Constructive Total Loss threshold | exceeds 75% of IDV | §7 ✅ |
| Owner-driver PA cover | Rs 15,00,000 | §8 ✅ |
| Driver under the influence | not covered | §4 ✅ |
| Depreciation on glass | NIL | §3 ✅ |
| Maternity expenses | *"The policy wording does not address this"* | correctly refused ✅ |

The last row matters most: asked about cover that isn't in the document, it **declined rather than
inventing a plausible number**. Tenant isolation held on the AI path too — asking about a product
version belonging to another tenant returns 404, never a cross-tenant answer.

### 5. Every wrong URL returned 500 instead of 404 (found during profile-by-profile UI testing)

While sweeping the Tenant Admin surface, `GET /api/v1/rulesets` (a path that doesn't exist — the real
one is `/rulesets/fraud`) returned **500**, not 404. Cause: Spring raises `NoResourceFoundException`
for an unmapped route, and the `@ExceptionHandler(Exception.class)` catch-all grabbed it and reported
`INTERNAL_SERVER_ERROR`. So **any** typo'd or renamed endpoint told the client "the server broke."
Added a handler returning `404 RESOURCE_NOT_FOUND`. Same family as bug #2: a catch-all that answers
500 masks client mistakes as server faults — every framework exception that means "you asked wrong"
(bad type, bad path, bad body) deserves its own 4xx handler ahead of the catch-all.

This one is worth the story because of *how* it was found: not by a test (tests hit the paths that
exist), but by fat-fingering the URL during exploratory testing. The bug lived precisely in the space
the test suite can't reach — requests for things that aren't there.

### 6. A claim could be assigned to someone who can't investigate (found testing the Investigation Manager)

The Investigation Manager's core power is `CLAIM_ASSIGN`. Testing it, I assigned a claim to the
**platform admin** — a user with no `CLAIM_INVESTIGATE` permission — and it returned 200 and moved the
claim to `UNDER_INVESTIGATION`. `assign()` checked only that the target user *existed*
(`findByIdAndIsDeletedFalse`), never that they could investigate. A claim could be stranded
`UNDER_INVESTIGATION` with an assignee (auditor, platform admin, even a customer) who has no way to act
on it. Fix: resolve the assignee's role permissions via the existing `PermissionService` and reject
with `400 NOT_AN_INVESTIGATOR` unless they hold `CLAIM_INVESTIGATE`. The subtlety worth saying out
loud: the endpoint's own `@PreAuthorize('CLAIM_ASSIGN')` guards *who may assign*; this is a separate
axis — *who may receive* an assignment — and authorization annotations don't express it.

### 7. Malformed request bodies returned 500 (found via a wrong enum value)

Posting `{"decision":"NONSENSE"}` to the decision endpoint returned **500**. An unknown enum constant
(or any unparseable JSON) raises `HttpMessageNotReadableException`, which — like bugs #2 and #5 — the
catch-all turned into a server error. That's three distinct framework exceptions
(`MethodArgumentTypeMismatch`, `NoResourceFound`, `HttpMessageNotReadable`) that all mean "the client
sent something wrong" and all defaulted to 500. Added a handler returning `400 MALFORMED_REQUEST_BODY`
(without echoing the raw Jackson message, which leaks type/package internals). The consolidated lesson
for the whole session: **a `@ExceptionHandler(Exception.class)` catch-all is a 500 factory** — it
silently reclassifies every unmapped client error as a server fault. The fix each time was one small
handler; the discipline is to add them proactively for the known 4xx-worthy framework exceptions
rather than discover them one 500 at a time.

### 8. `FRAUD_READ` was a dead permission — fraud scores leaked to every claim-reader ⭐

Testing the Investigator, I checked the `/claims/{id}/processing` endpoint (where the fraud score
lives) as **Customer Support** — a role deliberately *not* granted `FRAUD_READ`. It returned the full
`{score: 72, riskLevel: HIGH, explanation: "…"}`. Investigation showed `FRAUD_READ` — seeded in V14,
described "Read fraud scores," granted to exactly three investigation roles, and present in the
frontend permission constants — was **enforced in exactly zero places**. `grep FRAUD_READ src/main`
returns nothing. The endpoint that exposes fraud is gated by the much broader `CLAIM_READ`.

So the permission built to protect the fraud assessment did nothing, and a sensitive signal ("the
system flagged this claim as suspicious, 72/HIGH") was visible to Customer Support, Adjusters — anyone
with `CLAIM_READ`. Fix: gate the fraud block specifically on `FRAUD_READ` inside the query service,
while keeping processing *status* (OCR/analysis progress) under `CLAIM_READ`. `FRAUD_READ` holders see
the score; others get the same payload with `fraud: null`. Verified across five roles.

Two things make this the strongest security story of the session: (1) a permission existing is not a
permission enforced — a seeded, documented, role-mapped permission checked nowhere is worse than no
permission, because it *looks* like protection in every audit of the role table; and (2) the leak was
on a **different endpoint** than the obvious one — the claim detail correctly omits fraud, but the
processing view (a secondary surface) exposed it. Coarse-grained `CLAIM_READ` on an endpoint that
returns fine-grained sensitive data is where these leaks live.

### The profile-by-profile sweep — what each role proved

All 8 demo roles were driven end-to-end (UI via Playwright + API matrix) against the running app:

| Role | Verified | Bugs |
|---|---|---|
| Platform Admin | cross-tenant analytics, onboard, suspend, **impersonation** (audit-preserving) | #1 |
| Tenant Admin | user CRUD + login, org, correctly can't mint a platform admin | #5 |
| Investigation Manager | assigns (valid investigators only), can't decide/create | #6, #7 |
| Investigator | reads assigned claim, fraud (now gated), notes, coverage, decides | #8 |
| Customer | own claims only; **IDOR blocked both directions** (read + file); no staff access | none |
| Customer Support | raises claims for customers; fraud hidden; no assign/decide/user-mgmt | none |
| Employee/Adjuster | policies + coverage only; blocked from all claim ops | none |
| Auditor | analytics + audit + org reads; blocked from claims/processing/writes | none |

The lower-privilege boundaries (Customer, Support, Adjuster, Auditor) were **clean** — the RBAC model
is sound where it's enforced. Every bug clustered in the *staff* surface, and the two most serious
(#1 cross-tenant company, #8 dead FRAUD_READ) were both **"a guard that looks present but isn't"** —
exactly the failure the profile sweep is designed to catch and the test suite isn't.

### One design observation (not a bug): validation runs before authorization

Repeatedly, an unauthorized write with an incomplete body returned **400, not 403** — Spring runs the
controller's `@Valid` before the service-layer `@PreAuthorize`. With a *valid* body the same request
correctly 403s (verified for every role). It's standard Spring ordering and not a security hole (the
operation is still denied), but it does let an unauthenticated-for-this caller probe validation rules.
Worth stating in an interview as a known, accepted trade-off of authorizing at the service layer
rather than the controller — the codebase's deliberate choice (controllers stay thin, logic-free).

### Regression tests added (10 new: 7 `TenantIsolation`, 2 `ClaimWorkflow`, 1 `ProcessingQuery`)

Tenant admin can't list all companies (403) · auditor can't either (403) · can't read another tenant's
company (404) · can't overwrite it (404 **and the row is asserted unchanged** — a 404 that still
mutated would be the worse bug) · platform admin retains cross-tenant access (200) · `/companies/me`
resolves from the token · non-numeric id is 400 not 500.

One of them failed first time for a reason worth keeping: I sent a partial body and got 400, because
**bean validation runs before the ownership check**. A security test that never reaches the guard it
is testing is a false negative — the payload must be valid enough to get there.

---

*Maintenance note: extend this doc every phase. When we make a decision, add the **why** and the
**trade-off**. When we fix a bug, add a war story to Part 10 — those are the most memorable interview
answers.*

---

## Platform-admin & tenant-admin profile sweep — the "shared workspace" insight (verified in-browser)

**The architectural point that makes this worth an interview answer:** the app has exactly two route
groups — `app/platform/*` (platform-admin only: the cross-tenant tenants list) and `app/(app)/*` (the
**tenant workspace**: dashboard, claims, customers, policies, products, rulesets, notifications, and
the whole `organization/*` administration surface). A platform admin's **"Open workspace (impersonate)"**
mounts the *same* `(app)` components under a chosen tenant's context; a tenant admin logs straight into
them. There is **one** implementation of every workspace screen, not two. So every fix landed during the
platform-admin pass — inline field validation, `cursor-pointer` on all actionable controls, shadcn
tooltips on icon buttons, widened dialogs (`sm:max-w-2xl`), the sidebar active/hover gap, the
notification-bell unread refetch, and the **per-tenant RBAC editor** — applies to the tenant admin
automatically, because it *is* the tenant admin's screen. This is the payoff of putting tenancy in the
JWT + `@TenantId` and keeping the UI tenant-agnostic: the workspace doesn't know or care whether the
person driving it arrived by login or by impersonation.

**What was driven end-to-end (Playwright, real backend):**
- Demo-account sign-in — every card has a pointer + role tooltip.
- Platform overview + **tabbed tenant CRUD** (Active/Suspended/Archived/Deleted with live counts),
  icon actions each with a tooltip, all pointer-cursored.
- Onboard-tenant: a duplicate code correctly surfaces as a **top-level toast** (`TENANT_CODE_EXISTS`),
  not an inline field error — the deliberate split: *business-rule* violations → toast, *field-shape*
  violations → inline. (The tenant DTO carries only `@NotBlank`, which the client blocks, so it never
  produces an inline error — the inline path is proven on a form that has length validators.)
- Inline validation proven in-browser on the **region** form: a 300-char name renders
  *"Region name cannot exceed 255 characters"* directly under the field — the message coming straight
  from the backend `@Size` validator via `useMutation.fieldErrors` → `<FieldError>`, no hardcoded copy.
- **Per-tenant RBAC editor** in the workspace: every role editable, **Platform Admin is `locked`**
  (safety rail), the "applies to everyone here on their next request, doesn't affect other tenants,
  clearing resets to platform default" contract stated in the dialog, checkboxes grouped by module.
- Users list: 9 users with employee codes; the Customer login shows an **auto-generated** code
  (`DEMO-CUR`) — confirming the "customers are policyholders, not staff, so the code is synthesized"
  rule.

**Honest scope statement (what "tenant admin complete" does and doesn't mean):** the tenant admin's
**administration** surface — users, roles/permissions, regions/branches, departments, designations,
customers — is complete *and shared*, so it's genuinely done for both profiles. A full tenant-admin
sign-off still means walking the **operational** surface from that seat (the claims lifecycle,
policies, products, rulesets), which is the natural next profile pass rather than something the
administration sweep already covered.

---

## Tenant-admin operational pass — the "id shown as version number" bug (103 tests green)

Walked the full tenant-admin **operational** surface in the browser (claims list + the 6-tab claim
detail, policies, products, rulesets). Everything worked — the claim's Processing tab showed a live
fraud score of **72 (High)** with a plain-language reason, the policy detail carried the enriched
policyholder/product/vehicle/**wording-PDF**, the product's Knowledge dialog reported *"11 chunks
ingested · gemini-embedding-001"*, and the ruleset builder's rule dropdown listed all five catalog
rules including the three image-fraud ones. One real bug surfaced.

**The bug: a foreign-key id was being rendered as if it were the version number.** The claim Overview
showed *"Product version: 3"* and the Knowledge dialog titled itself *"Policy knowledge — version 3"*
— but `3` is the `insurance_product_version_id` (a PK), and the product `DEMO_MOTOR` has exactly one
version, **v1**. There is no "version 3." A user reading the claim would believe it was adjudicated
against a version that doesn't exist. The **policy** page had it right all along
(`v{productVersionNumber}` → "v1") because `PolicyMapper` resolves the id into a name + number; the
claim and the dialog were displaying the raw id instead.

**Why it matters (and why it's subtle):** the underlying *pinning* was correct — claim and policy
both point at version id 3 (D5 holds), so no test caught it and the data was sound. The defect was
purely in **presentation**: an internal identifier leaking into a human-facing "version N" label,
where N happens to look like a plausible version number. This is the display-layer cousin of the
"guard that looks present but isn't" theme — here it's *"a number that looks meaningful but isn't."*

**The fix (DRY, mirrors the policy path):** enrich `ClaimResponse` with `productName` +
`productVersionNumber`, resolved in `ClaimMapper` by injecting the product/version repositories and
looking up the pinned version — exactly the pattern `PolicyMapper` already used, so both surfaces now
resolve ids identically. The claim Overview shows *"Demo Motor Comprehensive"* + *"v1"*; the Knowledge
dialog takes the version *number* (which the product page already had in the row) instead of the id.
Signature of `toResponse(Claim, warnings)` was left unchanged, so all nine call sites needed no edit.
Verified in-browser and with the full suite: **103 passing.**

**Interview framing — the general lesson:** never let a database id reach a label that implies domain
meaning. If the UI says "version N", "claim #N", "case N", N must be the *business* number, not the
row's PK — resolve it in the mapper, once, so every caller is enriched the same way and no screen can
drift. The policy page and the claim page disagreeing for months is exactly what happens when that
resolution lives in one mapper but not its sibling.

---

## Customer (Policyholder) portal — closing the information-request dead-end (features batch)

The portal had a **dead end in the core loop**: when an investigator clicked "Request info", the claim
moved to `WAITING_FOR_CUSTOMER`, the customer's status line said *"please check your messages"* — but
there was no messages area, and the document-upload UI was gated behind `status === "DRAFT"`. The
backend already re-opened the claim when the customer uploaded a document (`recordCustomerResponseInternal`),
so the *server* supported the response; the *UI* gave the customer no way to trigger it. The loop
looked complete and wasn't.

Three features were batched to make the policyholder self-service actually round-trip:

1. **Respond to an information request.** When `WAITING_FOR_CUSTOMER`, an amber callout surfaces the
   investigator's actual message (carried on the transition into that status, in `claim_status_history`),
   and document upload is enabled in that state too — the button becomes "Upload response," and on
   upload the claim re-opens server-side and the page refreshes to `UNDER_INVESTIGATION`.

2. **Claim progress timeline.** A new ownership-scoped `GET /portal/claims/{id}/timeline` surfaces the
   status history (which was written all along but never read by any endpoint) as a customer-friendly
   tracker — Draft → Submitted → Under Investigation → Info requested → …, with notes and timestamps.

3. **Download my policy wording.** "My policies" was a read-only card with no actions. A policyholder
   can now download the wording PDF for the product version their policy is pinned to. The wrinkle:
   product-document read is gated by `PRODUCT_READ`, which a customer (PORTAL_* only) doesn't hold. So
   rather than loosen that permission, the portal service does its **own** ownership check (policy →
   belongs to this customer → its pinned version) and then calls a new **internal**
   `ProductDocumentService.listForVersionInternal` / `downloadForVersionInternal` that skips the staff
   gate. Downloads verify the requested document actually belongs to the customer's version (404
   otherwise), so a guessed id can't reach another version's file.

**Interview framing — two reusable ideas.** (a) *A backend capability with no UI trigger is not a
feature.* The reopen-on-upload logic existed and was even unit-tested, yet the product had a dead end,
because no screen could reach it — the profile-by-profile UI sweep is what surfaces that class of gap
the test suite structurally can't. (b) *Cross-permission reads belong behind an ownership check, not a
widened role.* The clean way to let a customer read a product document was a second, narrower gate
(ownership) plus an internal method, never granting the customer `PRODUCT_READ` — which would have
opened the entire product catalog to every policyholder.

---

## Global AI coverage assistant — the RAG on every screen, for every role (103 tests green)

The RAG assistant existed but was buried on one staff screen (the claim → Coverage tab), and the
**customer couldn't reach it at all** (the CUSTOMER role holds only PORTAL_* permissions, no
COVERAGE_READ). A policyholder had no way to ask "is windshield damage covered?" about their own
policy. Closed that gap with a **floating "Ask AI" assistant present on every screen of both the staff
app and the customer portal**, answering coverage & policy-wording questions from the RAG with
citations.

**Two product/security decisions drove the design:**

1. **Fraud rules are deliberately NOT in scope.** The obvious over-reach is "let everyone ask the AI
   anything, including how fraud scoring works." That hands a fraudster the evasion playbook — so the
   assistant answers *only* coverage/wording, and fraud rulesets stay on the staff Rulesets screen.
   The lesson: an AI assistant's *knowledge boundary* is a security control, not just a UX choice.

2. **"Every role" without widening permissions.** Coverage explanations are non-sensitive product
   wording, so `POST /coverage/ask` was relaxed from `hasAuthority('COVERAGE_READ')` to
   `isAuthenticated() and !hasAuthority('PORTAL_CLAIM_READ')` — i.e. *any staff member, but not a
   customer* (the `!PORTAL_CLAIM_READ` clause cleanly identifies "not a customer," since only that
   role has portal perms). Customers instead go through a separate ownership-scoped
   `POST /portal/coverage/ask {policyId, question}` that resolves the product version **from their own
   policy server-side** — so a policyholder can only ever ask about coverage they actually hold, and
   never supplies a version id. Verified: a customer hitting the staff endpoint gets **403** before any
   LLM call.

**The shape:** one role-agnostic `<AiAssistant>` component (a floating button + right-side Sheet) that
knows nothing about roles — it takes a `loadContexts()` and an `ask()` and renders a picker + chat +
citations. Two thin wrappers supply the difference: the customer wrapper lists *their policies* and
asks via the portal endpoint; the staff wrapper lists *products with ingested wording*
(`GET /coverage/products`) and asks via the staff endpoint. Same UI, different data + guard — no
role logic in the shared component.

**Quota reality (stated, not hidden):** Gemini free tier caps chat generation at 20/day. An assistant
on every screen amplifies usage, so the panel degrades gracefully — the model's honest "the wording
doesn't address this" and any 429 both surface as a normal assistant message rather than a crash.

---

## The missing hard validation — DUPLICATE_CLAIM_EXISTS (a real bug, found from odd demo data)

A "why are so many fraud scores 0?" question turned up a genuine bug. The 0 scores themselves were
**correct** — fraud score measures fraud *risk*, which is independent of the approve/reject decision
(a claim can be rejected for a coverage reason with zero fraud risk, and an approved claim *should*
be low-risk), and the engine's rules genuinely didn't fire (amount well under the sum insured, not an
early claim, no analysed images). The engine ran; it just found nothing.

But the *reason the data looked odd* was the tell: there were **six near-identical claims** — same
policy, same vehicle, same incident date, same amount. Per the design (D13), submitting an exact open
duplicate is supposed to be a **hard rejection** (`DUPLICATE_CLAIM_EXISTS`, "prevents accidental
double-submit"). The `ClaimSubmissionValidator` had the other three hard checks (future date, policy
active on loss date, claimant-is-policyholder, vehicle-matches-policy) but **not** the duplicate one —
so nothing stopped the same claim being filed six times, whether by a fat-fingered customer or a
fraudster.

**The fix:** at submit, look up other claims for the same policy + incident date, compare on the
**normalized** vehicle registration (so "MH 12 AB 1234" == "MH12AB1234"), exclude the claim being
submitted, and reject if any *non-terminal* one exists. Settled claims (APPROVED/REJECTED/CLOSED)
don't block a fresh claim — only still-open ones do, which is what "double-submit" means. Added
`ClaimStatus.isTerminal()` for that distinction. Verified: submitting a draft that duplicates an open
claim now returns `DUPLICATE_CLAIM_EXISTS`; a unique claim still submits; full suite 103 green.

**Interview framing:** "the fraud scores look wrong" was a red herring — the scores were right, but
chasing *why the underlying data looked that way* surfaced a missing guard the spec had called for
all along. Cheap-looking demo-data smells are worth one level of "why" before dismissing them.

---

## Profile, org placement, and claim reassignment (103 tests green)

Three gaps closed in one batch, plus an assignment explainer.

**How investigator assignment actually works** (the question that kicked this off): after submit, OCR
→ analysis → fraud run async; the claim then lands at `AWAITING_ASSIGNMENT` and *waits*. It is **not**
auto-assigned. A manager (`CLAIM_ASSIGN`) then either **Auto-assigns** (the engine picks the
least-loaded active investigator) or **Assigns** a specific one — the action itself is instant
(straight to `UNDER_INVESTIGATION`, no accept step). The delay is the async processing + the human
click, not the assignment.

1. **Profile section** — every role now has a "My profile" dialog (from the shared user menu, so it
   works in both the staff app and the customer portal): view identity + org placement, edit
   name/phone, and change password (current-password-gated). A dedicated `/profile` endpoint
   (`isAuthenticated()`, scoped to the caller's own id) rather than bloating the lightweight
   `MeResponse` used for UI gating.

2. **Department / designation / branch were saved but unused.** `AppUser` already had the FK columns;
   nothing set, exposed, or showed them. Wired end-to-end: pickers on the create/edit user form
   (staff only — a policyholder has no org placement), resolved to names in `UserResponse`, shown as
   columns on the users directory and on the profile. Added a flat `GET /organizations/branches` for
   the picker (branches were only listable per-region before).

3. **Claim reassignment** — the gap surfaced while explaining assignment: once assigned, there was no
   way to move a claim to a different investigator (`assign` only works from `AWAITING_ASSIGNMENT`).
   Added `POST /claims/{id}/reassign` (null id = auto-pick), which retires the live assignment
   (`REASSIGNED`), creates a new one, notifies, and records history — the claim stays
   `UNDER_INVESTIGATION`. Also fixed a latent UX quirk: Auto-assign/Assign were shown on
   already-assigned claims (where they'd 400); they're now gated to `AWAITING_ASSIGNMENT`, with
   Reassign shown for `UNDER_INVESTIGATION`.

**Interview framing:** #2 is the recurring "the schema supports it but no layer uses it" gap — the
data model had `department_id`/`home_branch_id` from day one, but without form fields, response
fields, and display it was invisible dead weight. Wiring a feature means all four layers, not just the
column.

---

## Investigating officer, employee-id pickers, and full multi-branch/region placement (103 tests green)

Four connected improvements to the org/assignment surface.

1. **Every claim now shows its investigating officer.** The claim detail exposed who *raised* a claim
   but not who was *assigned* to it — an investigation manager couldn't tell who owned a claim, even
   after auto-assign. `ClaimResponse` now carries `investigatingOfficerName/Code`, resolved in
   `ClaimMapper` from the live (ASSIGNED) `ClaimAssignment`; the Overview shows it, or "Not yet
   assigned".

2. **Assign/Reassign pickers show employee code, not email**, and the Select dropdown no longer
   spills wider than its trigger — the shared `ui/select` default flipped from `item-aligned`
   (content-sized) to `popper` with `w-(--radix-select-trigger-width)`, so every dropdown in the app
   matches its trigger width.

3. **Full multi-branch + region placement.** A staff user can now be assigned to a **region**
   (new `app_user.region_id`, migration V31) and to **several branches** (the long-dormant
   `user_branch_assignment` join table finally has an entity + repo), with one flagged home/primary.
   Wired through create/edit (region picker + branch checkboxes), `UserResponse`, and the profile
   ("Region", "Home branch (New Delhi)", "Branches: …").

**Migration war story worth keeping:** V31 first failed at Hibernate schema validation —
*"missing column deleted_at in table user_branch_assignment"*. The join table was created back in V4,
*before* the soft-delete convention, so mapping it via `TenantAwareEntity` (which requires
`is_deleted/deleted_at/deleted_by`) didn't validate. Worse, the first (broken) V31 had already
committed to the persistent dev/test DBs, so editing it triggered a Flyway **checksum mismatch**. Fix:
make V31 idempotent (`ADD COLUMN IF NOT EXISTS`, guarded `ADD CONSTRAINT`), delete its
`flyway_schema_history` row on both DBs, and let it re-apply cleanly. Lesson: a brand-new migration is
still "mutable" *only* by clearing its history row everywhere it touched — and reusing a pre-existing
table means inheriting whatever era's conventions it was born under.

---

## Full QA sweep — automated checks, cross-role Playwright, and responsiveness (2 fixes)

A whole-project verification pass.

**Automated (all green):** backend suite **103 tests / 0 failures**; frontend **production build** compiled
successfully and prerendered all 33 routes (catches prod-only issues dev mode hides); frontend
`tsc --noEmit` **0 errors**.

**Cross-role Playwright:** signed in per role and drove the major flows. Confirmed the recent work end
to end — navbar now shows **name + role** ("Manoj Manager / Investigation Manager"), the profile lists
all org placement (department, designation, region, home branch *with location*, and **multiple
branches**), a claim's **investigating officer** shows on the detail (the exact gap reported), and the
reassign picker shows **employee code** with the dropdown width matching its trigger.

**Responsiveness:** measured `scrollWidth > clientWidth` at 375 px per shell. The **staff app was
clean** (collapsible sidebar; tables scroll inside their own `overflow-x-auto` container; `min-w-0`
containment on the shell). A parallel code-audit + live measurement found the **customer portal header
overflowed** at 375 px (415 > 360 px) — the single-row nav didn't fit a phone, made worse by the new
name/role in the account menu. **Fixed:** brand text drops below `sm`, gaps/padding shrink, nav items
`shrink-0` + `whitespace-nowrap` → now 360 = 360, no horizontal scroll. Also hardened **10 dialogs**
that lacked `max-h-[90vh] overflow-y-auto` (tall multi-field forms could clip their submit button on
short/landscape phones) to match the pattern the other dialogs already used.

**Honest scope note:** this covered every major flow and both shells at desktop + mobile, not every
possible permutation (e.g. not every field-level validation on every form). No functional regressions
found; the two issues were both responsiveness polish, now resolved.

---

## Measuring the fraud scorer — an evaluation harness (tools/fraud-eval/)

To answer "how accurate is the fraud score?" honestly: you can't, without ground-truth labels. So built
a harness (`tools/fraud-eval/fraud_eval.py`) that makes the **methodology** concrete.

**What it does:** mirrors the engine's exact rules/weights/thresholds (from `FraudEngine.java`),
generates a labelled synthetic dataset (400 claims, 13% fraud base rate, documented per-rule fire
probabilities), scores each through the identical logic, and evaluates the score as a binary classifier
— all metrics computed from scratch (no sklearn).

**Headline results:** ROC **AUC 0.87**, AUPRC **0.66** (vs 0.13 no-skill baseline — 5× lift). At the
shipped **HIGH ≥50** cut-off: precision **0.90**, recall **0.37** (a precise, low-noise queue). At
**MEDIUM ≥25**: precision **0.56**, recall **0.67** (wider net, more false alarms). The score is
**monotonic with real fraud** — 0–19 band ≈ 3% fraud, 80+ band = 100%.

**The insight that sells it — per-rule lift:** `DUPLICATE_IMAGE` is 94% precise (when it fires it's
almost always fraud); `EARLY_CLAIM` fired 70× but was right only 34% of the time — it's dragging
MEDIUM's precision down. Actionable: demote its weight / make it a soft signal.

**Interview framing — the three things this demonstrates:** (1) you don't measure a rare-event
classifier with raw *accuracy* (a "never fraud" model is 87% accurate and useless) — you lead with
precision/recall/AUPRC; (2) the MEDIUM/HIGH thresholds are a *business dial* set from the ROC/PR curve,
not a fixed truth; (3) per-rule lift is how you tune a rule engine empirically. Honest caveat baked in:
labels are synthetic; the engine is rule-based *by design* (auditability), with a supervised-model
upgrade path once real investigator labels accrue.

Visual report artifact: rendered from `report.json` (calibration chart, ROC curve, confusion matrices,
per-rule lift). Rerun: `python tools/fraud-eval/fraud_eval.py`.

---

## Fraud evaluation — the live feedback loop + where the ruleset actually runs

**Feedback loop (built).** The eval harness started on synthetic labels; it now also runs on REAL
outcomes. Every claim *Decide* records a ground-truth label — `claim.fraud_confirmed` (migration V32):
approve → false, reject-for-fraud → true, reject-for-coverage → false. A new endpoint
`GET /fraud/evaluation/dataset` (`FRAUD_READ`) emits each decided claim's **score + fired rules
(parsed from the explanation) + true label**, and `python fraud_eval.py --live <url> <email> <pw>`
pulls it and runs the *identical* metrics. Proven end-to-end on seeded real decisions: 11 labelled
claims → AUC 0.81, MEDIUM precision 1.00 / recall 0.67, with one honest missed fraud (a fraud that
scored 0). Same report, synthetic → real, no code change — just more labels over time.

**Where the ruleset runs (the "who / which state" answer).**
- *Who configures it:* any role with `RULESET_WRITE` (Tenant Administrator has it) on the **Fraud
  Rulesets** screen — per-tenant, self-service. A ruleset = weighted rules + medium/high thresholds,
  scoped to a **claim type**, with a status; only the **ACTIVE** ruleset for that claim type applies
  (else built-in defaults).
- *Who consumes the output:* investigators / investigation managers, gated by `FRAUD_READ`.
- *Which state it's applied at:* automatically in the async pipeline, **once**, at the fraud-scoring
  step. `SUBMITTED → AWAITING_ANALYSIS` (OCR + image analysis run) → the atomic fraud **gate** fires
  only when *both* OCR and analysis are COMPLETE → `FraudEngine.evaluate()` loads the tenant's ACTIVE
  ruleset for the claim's type, scores, writes score+risk+explanation, and moves the claim to
  `AWAITING_ASSIGNMENT`. Not at draft, not at decision — right before assignment — and it re-runs on
  reprocessing when the customer responds with new information.

---

## Ruleset A/B comparison — tuning against outcomes (harness --compare)

The finish of the evaluation story: `python fraud_eval.py --compare config-default.json config-tuned.json`
re-scores the same labelled claims (synthetic or --live) under two ruleset configs and prints a
side-by-side metrics diff. A config is `{name, weights:{RULE:weight}, medium, high}` (omit a rule to
disable it). The bundled example acts on the harness's own finding — demote the noisy `EARLY_CLAIM`
(20→8), boost the sharp `DUPLICATE_IMAGE` (40→50) — and it **measurably wins**: AUPRC 0.662→0.681,
HIGH recall 0.365→0.385, HIGH F1 0.521→0.541. So the loop is complete: measure → find the weak rule →
tune offline against real outcomes → activate the winner. Turns the scorer from a black box into
something you can *improve with evidence*.

**Resilience note (asked & verified):** there is NO seeded default ruleset — the defaults are code
constants in `FraudEngine.java`. Proven: the demo tenant has 0 `fraud_ruleset` rows yet 12 scored
claims. Deleting a custom ruleset (soft-delete) → engine sees no ACTIVE ruleset → falls back to
defaults; a ruleset referencing an unknown rule code → `evaluator == null` guard skips it. Fraud
scoring cannot be broken by data deletion. (The only genuinely required seed is the global
roles/permissions, which the app never exposes for deletion.)

---

## Pagination — and the empty-page bug most implementations ship with

**The gap:** every list endpoint returned an unbounded `List<>`. `@TenantId` bounds a query to one
insurer, but a single insurer can still hold thousands of claims — so `GET /claims` would serialise
all of them and the UI would render all of them. Invisible at demo scale, a latency/memory problem at
production scale. The config lists (roles, departments, designations, regions, branches, products,
rulesets) are naturally small and deliberately stay unpaged; the **transactional** ones — claims,
customers, users, policies — are the ones that grow without bound, and those are what got paged.

**Wire format.** A `PagedResponse<T>` record — `content, page, size, totalElements, totalPages,
first, last` — mapped from Spring's `Page` rather than returning `Page`/`PageImpl` directly. Spring
Data does not treat `PageImpl`'s JSON shape as API contract; it has changed between versions, so
serialising it straight to clients couples your API to an internal detail. Endpoints take
`?page=&size=` and go through `PageRequests.of(...)`, which **clamps** — page floors at 0, size is
forced into [1, 100]. Without the clamp a negative page throws inside `PageRequest.of` and
`?size=100000` pulls the whole table, defeating the point of paging.

**The split that made it work: paged lists vs. `/options`.** The dropdowns (investigator picker,
customer picker on new-claim/new-policy) called the very same `list()` the screens did. Paginating
that endpoint would have silently truncated every picker to its first 10 entries — a data-loss bug
disguised as a UI change, and the kind of thing that only shows up once someone's 11th investigator
can't be assigned. So the listing endpoints paginate and dedicated `GET /users/options`,
`/customers/options`, `/policies/options` stay unpaged for pickers. Two different jobs, two
endpoints, neither compromised.

**The side effect that matters — the page that no longer exists.** You're on page 2 looking at the
11th row, you delete it, and a naive implementation refetches page 2, gets nothing back, and renders
an **empty table with no way out**. The fix lives in one place, `usePaginated`: after every fetch,
compare the current page against the returned `totalPages` and step back to the last page that still
has rows. Because it keys off server truth rather than a local guess, the same three lines cover
deleting the last row on a page, deleting a whole page's worth, and an out-of-range `?page=99` typed
straight into the URL. It can't loop — it only fires while `page` is strictly past the last index and
always targets a valid page. **Verified in the browser, not just reasoned about:** 11 customers →
page 2 showed "Showing 11–11 of 11" → deleted that row → the list landed back on page 1 with 10 rows
and the pagination bar auto-hid (one page left). The backend already behaves correctly here too — an
out-of-range page returns empty `content` with honest `totalElements`/`totalPages`, which is exactly
the signal the clamp needs.

**Cost:** 106 tests still green. One test changed — `UserListIntegrationTest` asserted the old array
shape and the `?role=` filter on `/users`; it now asserts the page envelope on `/users` and the role
filter on `/users/options`. That test failing was the system working: the contract moved, and the
suite noticed.
