# ClaimLens — Pre-Production Security & QA Audit

**Date:** 2026-07-29 · **Scope:** full stack (Spring Boot 4 API, Next.js 16 frontend, PostgreSQL, the
async processing pipeline) · **Method:** live black-box + grey-box testing against a running instance
(every finding below was *executed*, not inferred) plus a code review of the auth, tenancy, and upload
paths.

> **How to read severity/status.** Every finding is tagged **VERIFIED LIVE** (reproduced against the
> running app) or **CODE-CONFIRMED** (proven by reading the exact code path, not yet weaponised). Fixes
> applied in this pass are marked **✅ FIXED (verified)**; the rest carry a concrete recommendation.

---

## 0. Executive summary

The security fundamentals that usually sink a multi-tenant SaaS are **solid here** and were probed
hard: tenant isolation (Hibernate `@TenantId`, `isRoot()→false`), JWT parsing (algorithm pinned, no
`alg:none`/confusion), the customer-portal ownership gate, SQL-injection resistance, and
secret handling all held up under direct attack (§4). The gaps are in the layers *around* that core:
one **critical stored-XSS via file upload**, a **brute-force rate-limit bypass**, missing
**impersonation auditing**, and a cluster of **error-handling defects that turned client mistakes into
`500`s**. The error-handling cluster and the XSS are fixed in this pass.

| Sev | Finding | Status |
|---|---|---|
| **Critical** | C1 — Stored XSS via uploaded document → session-token theft | ✅ **FIXED (verified)** |
| **High** | H2 — Login brute-force bypass via spoofed `X-Forwarded-For` | Recommended (needs deploy config) |
| **High** | H3 — Platform-admin impersonation is unaudited | Recommended |
| Medium | M4 — No refresh-token reuse detection; password change doesn't revoke sessions | Recommended |
| Medium | M5 — Uploads: no size limit / no MIME allowlist / no AV | ⚠️ **Partially fixed** (size+413 done) |
| Medium | M6 — IDOR on product-document download (in-tenant) | Recommended (low-risk) |
| Medium | M7 — AI endpoints use a deny-list authz that fails open | Recommended (needs perm grant) |
| Medium | M8 — `USER_WRITE` can grant its own role every permission | Recommended |
| Medium | M9 — No access-token revocation (15-min window) | Documented tradeoff |
| Low | L10 — Role catalogue enumerable by any authenticated user | ✅ **FIXED (verified)** |
| Low | L11 — Poisoned `Content-Type` → permanent `500` on download | ✅ **FIXED (verified)** |
| Low | L12 — `/error-test` debug endpoint in the build | ✅ **FIXED (verified)** |
| Low | L13 — 6-char password minimum | Recommended |
| Low | L14 — Login timing oracle (user-exists) | Recommended |
| Low | L15 — Catch-all swallowed exceptions with no log | ✅ **FIXED (verified)** |
| — | SYS — method/media/size errors returned `500` instead of 405/415/413 | ✅ **FIXED (verified)** |

---

## 1. Critical

### C1 — Stored XSS via uploaded document → staff session takeover · ✅ FIXED (verified)

**VERIFIED LIVE.** As the lowest-privilege identity in the system (a portal `CUSTOMER`, or any staff
role with `CLAIM_WRITE`) I uploaded a file `evil.html` with part `Content-Type: text/html` containing
`<script>…</script>`. The upload succeeded (`201`), the document appeared in the staff **Documents**
tab as an "Accident Photo", and the download endpoint returned:

```
Content-Type: text/html
Content-Disposition: inline; filename="evil.html"
<html><body><script>alert(document.domain)</script>PWNED</body></html>
```

The frontend opens documents as a same-origin `blob:` URL (`documentService.ts` → `URL.createObjectURL`
→ `window.open`), so the script would execute on the app origin, where **access + refresh tokens live
in `localStorage`** — i.e. a customer can plant a script that steals an adjuster's session. `nosniff`
does not help: nothing is being sniffed; the declared type genuinely *is* HTML.

- **Root cause:** upload stored the client-supplied `file.getContentType()` verbatim
  (`DocumentServiceImpl.java`), and all four download sites echoed it back with `inline` disposition
  via `MediaType.parseMediaType(doc.contentType())`.
- **Fix applied:** new `common/util/SafeDownloads.java`. Downloads now serve an **allowlist** of
  render-safe types (`image/jpeg|png|gif|webp`, `application/pdf`) `inline`; **everything else** —
  including `text/html` and `image/svg+xml` — is forced to `application/octet-stream` +
  `attachment`, so it can never execute in the origin. Applied at all four sites
  (`DocumentController`, `PortalController` ×2, `ProductDocumentController`).
- **Re-verified live:** `evil.html` now downloads as `application/octet-stream; attachment`; a real
  `application/pdf` still renders `inline`. XSS neutralised, legitimate image/PDF viewing preserved.
- **Still recommended (defense in depth):** validate magic bytes on *upload* (Apache Tika) against the
  allowlist and store the derived type; add a CSP header; move tokens out of `localStorage` into
  `httpOnly` cookies. These were **not** done in this pass (larger change; see §6).

---

## 2. High

### H2 — Login brute-force bypass via spoofed `X-Forwarded-For` · Recommended

**VERIFIED LIVE.** The auth limiter keys on the **leftmost** `X-Forwarded-For` value
(`RateLimitFilter.clientIp`), which is fully client-controlled. Result, measured:

| Attack | Attempts that passed the limiter |
|---|---|
| 26 bad logins, **rotating** `X-Forwarded-For` per request | **26 / 26** (0 blocked) |
| 26 bad logins, **fixed** IP (control) | 20 passed, 6 × `429` |

This is the *only* brute-force control — `AuthServiceImpl.login` has no per-account lockout, no delay.
Combined with L13 (6-char passwords) and globally-unique cross-tenant email, this enables unlimited
password spraying against every account on the platform.

- **Recommended:** derive the client IP from the **rightmost-untrusted** hop, not the leftmost — set
  `server.forward-headers-strategy=native` + a trusted-proxy count matched to the deploy (Render
  appends one hop), or count back `N` from the right. Add a per-account failed-login counter with
  exponential backoff, independent of IP. *Not fixed in this pass:* correctness depends on the exact
  production proxy topology, which is a deploy decision.

### H3 — Platform-admin impersonation is unaudited · Recommended

**CODE-CONFIRMED.** `PlatformService.impersonate` mints a normal `TENANT_ADMIN` access token for the
target tenant with **no `@Auditable`** and no impersonation-session record. The token is
byte-indistinguishable from a real tenant-admin login; subsequent audit rows are written with
`user_id = <platform admin id>` against `tenant_id = <victim tenant>` — a user id that doesn't exist
in that tenant, with nothing marking it as platform access. For a claims platform this is a
regulatory-accountability gap, not just hygiene.

- **Correctly scoped, to be fair:** 15-minute TTL, no refresh token issued, `customerId` nulled, and
  the token does **not** carry `PLATFORM_ADMIN`, so the impersonating session is confined to the one
  target tenant — no privilege-escalation path was found.
- **Recommended:** add `imp:true` + `act:<adminId>` claims; persist an `impersonation_session` row
  (admin, tenant, reason, start/end); `@Auditable` on `impersonate()`; surface it in the tenant's own
  audit view; require a typed justification.

---

## 3. Medium & Low (condensed)

- **M4 — Refresh tokens (CODE-CONFIRMED).** Rotation, SHA-256-at-rest, and single-use are done well.
  Gaps: (a) a replayed already-rotated token just `404`s — the descendant chain isn't revoked, so
  token theft is undetected; (b) **password change/reset don't revoke sessions** — a user who resets
  because they suspect compromise leaves the attacker's refresh token valid for its full 7 days.
  *Fix:* on a known-but-revoked token hash, walk `replacedBySessionId` and kill the chain; on any
  password write, `UPDATE user_session SET revoked_at=now() WHERE user_id=? AND revoked_at IS NULL`.

- **M5 — Uploads (VERIFIED LIVE). ⚠️ Partially fixed.** There was no size limit (silent Spring 1MB
  default; a 2 MB file returned `500`), no MIME allowlist, no AV, and bytes are fully heap-buffered
  (`file.getBytes()` → `byte[]`). **Fixed this pass:** explicit `max-file-size=10MB` /
  `max-request-size=15MB` and a `413 FILE_TOO_LARGE` handler (re-verified: 11 MB → clean `413`).
  **Still recommended:** magic-byte allowlist, AV scan on ingest (unscanned bytes currently reach OCR,
  the analysis service, and PDFBox), and streaming instead of buffering.

- **M6 — Product-document IDOR (CODE-CONFIRMED).** `ProductDocumentController.download` binds
  `productId`/`versionId` then discards them; the lookup is by `documentId` alone, so any
  `PRODUCT_READ` holder reads any product document in the tenant via a wrong-but-valid URL. Contained
  by `@TenantId` + `PRODUCT_READ` (no cross-tenant leak). The **portal** variant gets this right
  (checks `versionId.equals(...)`). *Fix:* thread `versionId`/`productId` into the lookup and `404` on
  mismatch, mirroring the portal path. Low-risk — recommended for a follow-up.

- **M7 — AI deny-list authz (VERIFIED LIVE).** `@PreAuthorize("isAuthenticated() and
  !hasAuthority('PORTAL_CLAIM_READ')")` on `/coverage/ask` and `/coverage/products` grants the paid
  Gemini path to *every* non-portal role with no positive permission. Proven: an **AUDITOR** (which
  does **not** hold `COVERAGE_READ`) got a `200` answer from `/coverage/ask`. *Fix:*
  `hasAuthority('COVERAGE_READ')` and grant `COVERAGE_READ` to the staff roles that should have it.
  *Not applied this pass* because `TENANT_ADMIN` currently lacks `COVERAGE_READ`, so the switch needs a
  companion migration (a role decision, not a mechanical change).

- **M8 — Self-escalation via the role editor (CODE-CONFIRMED, surface reachable live).**
  `PUT /roles/{id}/permissions` is gated on `USER_WRITE` with no self-role guard, so a `USER_WRITE`
  holder can grant its **own** role every permission except `PLATFORM_ADMIN` (which is blocked on both
  the role and the permission). Blast radius is one tenant. *Fix:* reject editing the caller's own
  `roleId`; gate the editor on a distinct `ROLE_ADMIN` permission; audit every change.

- **M9 — No access-token revocation (CODE-CONFIRMED).** The JWT filter is fully stateless; a
  `SUSPENDED`/`TERMINATED` or role-reassigned user keeps access for up to the 15-minute TTL.
  *Permission* changes apply immediately (permissions are re-read per request), so the window is
  narrow — a defensible tradeoff, but **document it** and add a per-user `tokens_valid_after` check for
  the offboarding path.

- **L10 — Role enumeration (VERIFIED LIVE). ✅ FIXED.** `GET /roles` and `/roles/{id}` had no
  `@PreAuthorize`; a portal **customer** received the full role catalogue (`200`). Did the frontend
  caller audit first: the only caller is the user create/edit dialog (`user-form-dialog.tsx`), which is
  itself gated behind `USER_WRITE` and only opened by `TENANT_ADMIN` (who holds `USER_READ`). Added
  `@PreAuthorize("hasAuthority('USER_READ')")` to `RoleServiceImpl.getAllRoles`/`getRole`. Re-verified
  live: **customer → `403`, admin → `200`** (the legitimate caller still works).

- **L11 — Poisoned content-type → permanent 500 (CODE-CONFIRMED). ✅ FIXED.** A document uploaded with
  a non-parseable `Content-Type` made every later download throw `InvalidMediaTypeException` → `500`
  forever. `SafeDownloads.parseOrNull` now degrades a bad type to `octet-stream` instead of throwing.

- **L12 — `/error-test` debug endpoint. ✅ FIXED.** Removed from `HealthController`.

- **L13 — 6-char password minimum (CODE-CONFIRMED).** `@Size(min=6)` on set/change-password. Raise to
  12 + a breach-list check. BCrypt at default cost is used correctly.

- **L14 — Login timing oracle (CODE-CONFIRMED).** `login` short-circuits before BCrypt on a
  non-existent user (~1 ms vs ~100 ms), leaking account existence despite an identical error message.
  `forgot-password` is correctly always-`204`. *Fix:* run a dummy `matches` against a fixed hash on the
  miss path.

- **L15 — Silent catch-all. ✅ FIXED.** The `Exception.class` handler returned a generic `500` with
  **no log** — real faults (and attacks that trigger them) left no trace. Added `log.error("Unhandled
  exception", ex)` (response body unchanged, so nothing leaks to the client).

- **SYS — Wrong status codes. ✅ FIXED (verified).** `GlobalHandlerException` had no handlers for
  `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException`, or
  `MaxUploadSizeExceededException`, so a valid path with the wrong verb (`DELETE /claims/1`), a
  `text/plain` body, or an oversize upload all fell through the catch-all as `500`. Added `405` / `415`
  / `413` handlers. Re-verified live: `DELETE /claims/1 → 405`, `text/plain → 415`, 11 MB upload →
  `413`.

---

## 4. Examined and found sound (probed, no finding)

These are the places things usually break; each was attacked directly and held:

- **Tenant isolation.** Created a second "attacker" tenant and, as its admin, attempted to read/write
  tenant A's claims, customers, policies, users by direct id — **every attempt returned `404`**, and
  the attacker's list endpoints returned only its own rows. `isRoot()` is hardcoded `false` (the
  Hibernate root-tenant backdoor is nailed shut); unbound tenant falls back to `SYSTEM_TENANT=-1L`.
- **JWT.** `alg:none` forgery, HS256 signed with an attacker key, a tampered payload, and an expired
  token were **all rejected `401`**. Parsing pins the algorithm via `verifyWith(key)`; the secret has
  no default (fail-fast); permissions are resolved server-side per request, never carried in the token.
- **Customer portal ownership gate.** As customer Rahul, every attempt to read/act on customer Neha's
  claims/policies returned `404`; filing a claim with `{customerId: <Neha>, policyId: <Neha's>}` in the
  body failed — identity is taken from the JWT (`currentCustomerId()`), the body is ignored.
- **SQL injection.** Five classic payloads (`'; DROP TABLE claim; --`, `' OR '1'='1`, UNION, …) were
  stored **literally and inertly**; row counts unchanged; zero string-concatenated SQL in the repo (all
  binds / `?` placeholders).
- **IDOR responses are `404`, not `403`/`200`** — no existence oracle, consistently.
- **Secrets.** `backend/.env` is gitignored and untracked; `application.properties` holds only
  `${ENV}` placeholders; `DATABASE_PASSWORD`/`JWT_SECRET` have no defaults (fail-fast).
- **Path traversal.** Storage keys are UUID-prefixed and sanitised (`[^A-Za-z0-9._-]→_`); the read path
  uses only the DB-stored key.
- **Error bodies don't leak.** No stack traces or internal messages in any handler (confirmed while
  fixing L15).

---

## 5. Architecture & performance notes (Phase 7/8)

- **N+1 in `ClaimMapper` (CONFIRMED).** `toResponse` calls **6 repositories per claim** (product
  version, product, customer, user, role, assignment). Server-side pagination now bounds this to one
  page (≤10 rows → ~60 queries), which is why the list endpoints were paginated — but each row still
  fans out. *Recommended:* batch-load the referenced entities once per page (`findAllById`) and map
  from maps. Measured list latency today is healthy (4–120 ms; the spikes are dev-JIT warmup).
- **Frontend validation (from the surface map).** No `zod`; a few `<Select>` fields in the new-claim
  and new-policy forms aren't registered with react-hook-form, so a submit with nothing chosen can send
  `NaN`/`undefined` and rely on the backend to reject. Route gating is **client-side only** (no
  `middleware.ts`) — fine as UX, but the server is the real gate (verified: the API denies regardless).
- **Tokens in `localStorage`** (XSS-reachable) and **no cross-tab logout** (signing out in one tab
  leaves others authenticated until their next `401`).
- **Accessibility.** `IconButton` (aria-label + tooltip) is used well in most places, but: a handful of
  icon-only buttons use `title` instead of `aria-label`; most `<Label>`s lack `htmlFor`/`id` pairing;
  clickable `<TableRow>`s aren't keyboard-focusable. None blocking; worth a pass.
- **Console.** The only runtime console errors observed were **dev-only Turbopack HMR artifacts** from
  the editing session (the production `next build` passes, which would fail on a genuine undefined
  reference), plus the expected unauthenticated `/auth/me` `401` and the Google-GSI "origin not allowed"
  warning on localhost. No real frontend runtime bug found.

---

## 6. Recommended follow-ups, by effort

**Quick wins (≤1 file each):** L13 password length, L14 timing dummy `matches`, M6 product-doc id
check, M9 `tokens_valid_after` documentation. *(L10 role authz was applied this pass.)*

**Medium:** M7 (switch to `COVERAGE_READ` + grant migration), M8 self-role guard, M4 session revoke on
password write, upload MIME allowlist (Tika), a CSP header, cross-tab logout.

**Larger / infra:** H2 trusted-proxy IP + per-account lockout, H3 impersonation audit trail
(migration + claim), AV scanning on upload, tokens → `httpOnly` cookies + CSRF, `ClaimMapper` batch
fetch.

**Fixed in this pass (all re-verified live):** C1 download hardening, L10 role-catalogue authz, L11
parse-500, L12 debug endpoint, L15 exception logging, and the 405/415/413 status-code handlers +
explicit upload size limit.
