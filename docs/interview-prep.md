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

*Maintenance note: extend this doc every phase. When we make a decision, add the **why** and the
**trade-off**. When we fix a bug, add a war story to Part 10 — those are the most memorable interview
answers.*
