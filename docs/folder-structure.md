# ClaimLens — Folder Structure

Repository layout as-built (2026-07-29). Reflects the real tree on disk.

## Repository root

```
Claim-Lens/
│
├── backend/               # Spring Boot 4 / Java 21 API (Maven, Flyway, Postgres)
├── frontend/              # Next.js 16 App Router (React 19, Tailwind 4, shadcn/ui)
├── analysis-service/      # FastAPI image-analysis service (OpenCV, imagehash, Pillow)
├── ocr-service/           # optional Python OCR service (Tesseract)
├── tools/                 # dev/eval tooling (fraud-eval harness)
├── infra/                 # local infra assets (docker, monitoring, nginx, scripts)
├── docs/                  # design + as-built documentation
├── dummy documents/       # sample claim documents for demos/testing
│
├── docker-compose.yml     # local Postgres/Redis/services
├── render.yaml            # Render blueprint (backend + analysis-service)
├── deploy.md              # deployment steps (Render / Vercel / Neon / Upstash / R2)
├── setup.md               # local setup guide
├── .gitignore
└── README.md
```

## Backend — `backend/`

```
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/niyotechnologies/claimlens/
    │   │   ├── ClaimlensApplication.java
    │   │   │
    │   │   ├── config/            # DemoDataSeeder + cache/database/web config
    │   │   ├── security/          # JWT filter, rate limiting, authz, security config
    │   │   ├── tenancy/           # @TenantId resolver, TenantContext, tenant filter
    │   │   ├── common/            # BaseEntity/TenantAwareEntity, PagedResponse, utils, error handler
    │   │   │
    │   │   ├── auth/              # login, refresh, invitation/reset, Google sign-in
    │   │   ├── role/              # roles + permissions (global; tenant overrides)
    │   │   ├── user/              # AppUser, sessions, branch/region assignments
    │   │   ├── organization/      # InsuranceCompany, Region, Branch, Department, Designation
    │   │   │
    │   │   ├── customer/          # Customer
    │   │   ├── product/           # InsuranceProduct(+Version), ProductDocument
    │   │   ├── policy/            # InsurancePolicy, InsuredVehicle
    │   │   ├── claim/             # Claim, status history, claim types, lifecycle
    │   │   ├── document/          # Document + DocumentVersion (upload/download)
    │   │   ├── coverage/          # AI coverage Q&A (CoverageAnswer/Citation, PolicyChunk)
    │   │   │
    │   │   ├── assignment/        # ClaimAssignment (accept/reassign)
    │   │   ├── investigation/     # InvestigationNote
    │   │   ├── processing/        # OCR + analysis + fraud-gate orchestrator + worker
    │   │   ├── fraud/             # FraudEngine, rules, FraudRuleset, FraudScore/Job
    │   │   ├── ruleset/           # fraud ruleset management
    │   │   │
    │   │   ├── notification/      # Notification (in-app + email)
    │   │   ├── analytics/         # dashboard/analytics read models
    │   │   ├── audit/             # AuditLog
    │   │   ├── events/            # (empty placeholder — no event bus in V1)
    │   │   ├── outbox/            # (empty placeholder — no outbox table in V1)
    │   │   ├── scheduler/         # (empty placeholder — the live @Scheduled worker is in processing/)
    │   │   ├── integration/       # external service clients (AI, OCR, storage, email)
    │   │   │
    │   │   ├── portal/            # customer self-service portal endpoints
    │   │   └── platform/          # platform console (cross-tenant admin)
    │   │
    │   └── resources/
    │       ├── application.properties
    │       ├── application-prod.properties
    │       ├── demo-seed.sql          # DEMO_SEED=true "Demo Insurance" tenant
    │       ├── db/migration/          # Flyway V1__ … V32__ (see below)
    │       ├── templates/             # email templates
    │       └── static/
    └── test/                          # 106 integration/unit tests (real Postgres claimlens_test)
```

### Typical module layout

Each domain package is a self-contained slice. Layout varies by module; a full one looks like:

```
<module>/
├── controller/     # REST endpoints
├── service/        # interface + Impl business logic
├── repository/     # Spring Data JPA repositories
├── entity/         # JPA entities
├── enums/          # status/type enums
├── dto/            # request/response DTOs
├── mapper/         # entity ↔ DTO mappers
├── validator/      # request validators
└── support/        # helpers / specifications
```

Some modules carry specialised sub-packages, e.g.:

```
processing/  → analysis/  client/  ocr/  orchestrator/  worker/  support/
fraud/       → engine/    rule/
coverage/    → ai/
auth/        → google/    support/
security/    → filter/    ratelimit/  model/  config/  constants/
tenancy/     → context/   filter/     model/
```

### Flyway migrations — `backend/src/main/resources/db/migration/` (V1–V32)

```
V1  initial_schema              V17 audit_tables
V2  organization_tables         V18 notification_analytics_tables
V3  access_control_tables       V19 user_read_permission
V4  user_tables                 V20 ocr_result_tables
V5  permission_seed             V21 analysis_result_tables
V6  auth_tables                 V22 policy_ai_tables
V7  customer_tables             V23 platform_admin
V8  reference_data_tables       V24 user_write_permission
V9  product_tables              V25 document_version_tables
V10 insurance_policy_tables     V26 customer_portal
V11 claim_tables                V27 user_invitation_tables
V12 document_tables             V28 tenant_archived_status
V13 claim_assignment_tables     V29 product_document_tables
V14 processing_tables           V30 tenant_role_permission
V15 fraud_ruleset_tables        V31 app_user_region
V16 investigation_tables        V32 claim_fraud_confirmed
```

## Frontend — `frontend/`

Next.js App Router. Root-level `app/`, `components/`, `lib/`, `services/`, `hooks/`, `store/` — **no `src/` directory**.

```
frontend/
├── app/                        # App Router routes
│   ├── layout.tsx  page.tsx  globals.css  icon.svg  opengraph-image.tsx
│   ├── sign-in/                # login
│   ├── set-password/           # invitation / reset flow
│   ├── blog/  roadmap/         # marketing/content pages
│   │
│   ├── (app)/                  # STAFF app (route group, authenticated shell)
│   │   ├── layout.tsx
│   │   ├── dashboard/
│   │   ├── claims/  customers/  policies/  products/
│   │   ├── organization/  rulesets/  notifications/
│   │
│   ├── portal/                 # CUSTOMER self-service portal
│   │   ├── layout.tsx  page.tsx
│   │   └── claims/  policies/
│   │
│   └── platform/               # PLATFORM console (cross-tenant admin)
│       ├── layout.tsx  page.tsx
│       └── tenants/
│
├── components/                 # feature + shared UI components
│   ├── ui/                     # shadcn/ui primitives (Radix)
│   ├── ai/  auth/  claims/  customers/  organization/
│   ├── policies/  products/  rulesets/  platform/  users/
│   ├── notifications/          # (+ notifications-bell.tsx)
│   ├── app-sidebar.tsx  app-footer.tsx  page-header.tsx
│   ├── data-table.tsx  data-state.tsx  pagination-bar.tsx
│   ├── can.tsx  status-badge.tsx  confirm-dialog.tsx  profile-dialog.tsx
│   ├── impersonation-banner.tsx  google-signin.tsx  demo-accounts.tsx
│   └── providers.tsx  theme-toggle.tsx  user-menu.tsx  …
│
├── services/                   # axios API clients (one per domain)
│   ├── authService.ts  claimService.ts  customerService.ts  policyService.ts
│   ├── productService.ts  coverageService.ts  documentService.ts
│   ├── investigationService.ts  organizationService.ts  processingService.ts
│   ├── rulesetService.ts  userService.ts  portalService.ts  platformService.ts
│   └── profileService.ts  miscService.ts
│
├── hooks/
│   ├── useAuth.ts  usePaginated.ts  useAsync.ts  useMutation.ts
│   └── redux.ts  use-mobile.ts
│
├── store/                      # Redux Toolkit (auth only)
│   ├── index.ts  authSlice.ts
│
├── lib/                        # types, http client, enums, permissions, formatters
│   ├── http.ts  types.ts  enums.ts  permissions.ts  navigation.ts
│   ├── format.ts  dayjs.ts  tokenStorage.ts  blog.ts  utils.ts
│
├── content/  public/           # blog/marketing content + static assets
├── package.json  tsconfig.json  next.config.ts  eslint.config.mjs
├── components.json  postcss.config.mjs
└── CLAUDE.md  AGENTS.md  README.md
```

## Analysis service — `analysis-service/`

```
analysis-service/
├── app/
│   ├── main.py            # FastAPI app
│   ├── config.py
│   ├── hashing.py  similarity.py  exif.py  synthetic.py   # image analysis
│   └── models.py
├── tests/
├── requirements.txt  requirements-dev.txt
├── Dockerfile
└── README.md
```

## Tools — `tools/fraud-eval/`

```
tools/fraud-eval/
├── fraud_eval.py                 # fraud-rule evaluation harness
├── config-default.json  config-tuned.json
├── report.html  report.json
└── README.md
```

## Docs — `docs/`

```
docs/
├── domain-model.md                          # authoritative as-built domain model
├── ClaimLens_Complete_Domain_Model_v1.2.md  # SUPERSEDED historical snapshot
├── architecture.md  product-vision.md  v1-scope.md
├── buisness-flows.md  audit-report.md  interview-prep.md  folder-structure.md
├── ai-layer/  api-docs/  architecture … (grouped subfolders)
├── database-design-docs/  event-docs/  implementation-docs/
├── product-domain/  service-docs/
```
