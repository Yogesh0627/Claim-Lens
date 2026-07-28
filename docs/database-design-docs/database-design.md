> **⚠️ Design-era document — reconciled against the as-built schema on 2026-07-29.** This is an iterative pre-implementation draft. The authoritative schema is the **Flyway migrations `V1__…V32__`** (backend/src/main/resources/db/migration) and [`../domain-model.md`](../domain-model.md). Where this draft diverges, the migrations win; key deltas are flagged inline as **As-built** notes.

# ClaimLens - Database Design

Status: Draft

Version: 1.0

Owner: Niyo Technologies

Last Updated: June 2026

---

# Purpose

This document defines the physical database design of ClaimLens V1.

It serves as the source of truth for:

* PostgreSQL Schema Design
* Table Design
* Relationships
* Constraints
* Indexes
* Partitioning
* Retention Policies
* Migration Standards

This document converts the Domain Model into a production-ready PostgreSQL implementation.

---

# Database Technology

Database Engine

PostgreSQL 16+

Reason:

* ACID Compliance
* Strong Consistency
* Excellent Indexing
* JSONB Support
* Full Text Search Support
* Enterprise Maturity

---

# Database Strategy

Architecture:

Single Shared Database

Database Name:

claimlens_db

Multi-Tenant Strategy:

Shared Database
+
Tenant Isolation via tenant_id

All tenant-scoped tables must contain:

tenant_id BIGINT NOT NULL

---

# Schema Strategy

**As-built (2026-07-29):** the shipped database uses a **single `public` Postgres schema** only. The separate `processing`, `audit`, and `analytics` schemas described below were NOT created — all tables live in `public`. Also, several tables named in these examples were never built: `audit_event` (only append-only `audit_log` exists), `analytics_snapshot`, `dashboard_metric`, and `notification_job` (notifications are a single `notification` table). See the per-domain As-built notes for authoritative table lists.

Database Schemas

public

Purpose:

Core Business Tables

Examples:

* insurance_company
* region
* branch
* user
* customer
* claim
* document

---

processing

Purpose:

Background Processing

Examples:

* ocr_job
* analysis_job
* fraud_job
* notification_job
* claim_processing_state
* ocr_result

---

audit

Purpose:

Compliance Tracking

Examples:

* audit_event
* audit_log

---

analytics

Purpose:

Aggregated Data

Examples:

* analytics_snapshot
* dashboard_metric

---

# Naming Conventions

Tables

snake_case

Examples:

insurance_company

claim

claim_history

document_version

---

Columns

snake_case

Examples:

tenant_id

claim_number

created_at

updated_at

---

Indexes

idx_<table>_<column>

Examples:

idx_claim_tenant

idx_claim_status

idx_assignment_investigator

---

Unique Constraints

uq_<table>_<column>

Examples:

uq_claim_claim_number

uq_user_email

---

Foreign Keys

fk_<table>_<reference>

Examples:

fk_claim_customer

fk_document_claim

---

# Primary Key Strategy

All tables use:

BIGSERIAL

Example:

id BIGSERIAL PRIMARY KEY

Reason:

* Faster Indexes
* Smaller Storage
* Better PostgreSQL Performance

---

# Business Identifier Strategy

**As-built (2026-07-29):** PKs are `BIGSERIAL`/`BIGINT` (`Long`) as stated. In addition, externally-addressable aggregates (e.g. `claim`, `document`, `customer`) also carry a `public_id UUID` column (used in URLs) — internal `id` is never exposed, `public_id` is.

Internal IDs are never exposed.

Business identifiers are exposed.

Examples:

Claim

claim_number

CLM-2026-000001

---

Customer

customer_number

HDFC_CUST_000001

---

Employee

employee_code

HDFC_INV_000001

---

# Tenant Strategy

**As-built (2026-07-29):** multi-tenancy is enforced via a Hibernate `@TenantId` discriminator (`tenant_id`) on tenant-scoped tables (`TenantAwareEntity`). `InsuranceCompany`, `Role`, and `Permission` are **global** (extend `BaseEntity`, no `tenant_id`) — Role/Permission are shared platform catalogs, with per-tenant overrides in `tenant_role_permission` (V30).

InsuranceCompany acts as the tenant root.

InsuranceCompany.id is the tenant identifier.

Every tenant-scoped table contains:

tenant_id BIGINT NOT NULL

---

Mandatory Index:

CREATE INDEX idx_<table>_tenant
ON <table>(tenant_id);

---

# Common Audit Columns

All mutable business tables contain:

created_at TIMESTAMP NOT NULL

created_by BIGINT

updated_at TIMESTAMP

updated_by BIGINT

---

# Soft Delete Strategy

All business tables contain:

is_deleted BOOLEAN NOT NULL DEFAULT FALSE

deleted_at TIMESTAMP

deleted_by BIGINT

Reason:

* Auditability
* Recovery
* Compliance

Hard deletes should be avoided.

---

# Timestamp Standards

Store all timestamps in UTC.

Examples:

created_at

updated_at

submitted_at

closed_at

---

# Relationship Strategy

One-to-One

Use unique foreign keys.

---

One-to-Many

Use standard foreign keys.

---

Many-to-Many

Use mapping tables.

Example:

user_branch_assignment

---

# JSONB Usage Rules

Allowed Only For:

Flexible Configuration

Examples:

branding_config

rule_parameters

metadata

notification_payload

---

Never Store:

Core Relational Data

inside JSONB.

---

# Indexing Strategy

Every table:

Primary Key Index

---

Tenant Tables:

tenant_id index

---

Common Filters:

status

created_at

assigned_user_id

claim_number

---

Composite Index Examples

(tenant_id, status)

(tenant_id, created_at)

(tenant_id, claim_number)

---

# Partitioning Strategy

V1

No Table Partitioning

Reason:

Simpler Operations

Lower Complexity

---

Future

Partition Candidates:

audit_log

analytics_snapshot

notification

---

# Transaction Guidelines

Keep transactions short.

Avoid:

Long Running Transactions

Large Batch Locks

Cross-Service Transactions

---

# Job Processing Tables

All processing jobs follow:

PENDING

PROCESSING

COMPLETE

FAILED

---

Common Fields

id

tenant_id

status

attempt_count

locked_at

started_at

completed_at

failure_reason

created_at

updated_at

---

# Worker Recovery Strategy

Worker claims jobs using:

FOR UPDATE SKIP LOCKED

Recovery process:

Find jobs:

status = PROCESSING

AND

locked_at older than threshold

↓

Reset to PENDING

↓

Retry

---

# OCR Storage Strategy

Raw OCR payloads are NOT stored in PostgreSQL.

Stored In:

AWS S3

Example:

ocr-result.json

---

PostgreSQL stores:

payload_location

confidence_score

processing_time_ms

document_version_id

---

# Backup Strategy

Daily Backups

Retention:

30 Days

---

Disaster Recovery

Point-In-Time Recovery Enabled

---

# Migration Strategy

Tool:

Flyway

Naming:

V1__initial_schema.sql

V2__organization_tables.sql

V3__claim_tables.sql

etc.

**As-built (2026-07-29):** the authoritative migration set is **V1–V32**, and V3 is `access_control_tables` (NOT `claim_tables`; claims land at V11). Exact ordered list: V1 initial_schema, V2 organization_tables, V3 access_control_tables, V4 user_tables, V5 permission_seed, V6 auth_tables, V7 customer_tables, V8 reference_data_tables, V9 product_tables, V10 insurance_policy_tables, V11 claim_tables, V12 document_tables, V13 claim_assignment_tables, V14 processing_tables, V15 fraud_ruleset_tables, V16 investigation_tables, V17 audit_tables, V18 notification_analytics_tables, V19 user_read_permission, V20 ocr_result_tables, V21 analysis_result_tables, V22 policy_ai_tables, V23 platform_admin, V24 user_write_permission, V25 document_version_tables, V26 customer_portal, V27 user_invitation_tables, V28 tenant_archived_status, V29 product_document_tables, V30 tenant_role_permission, V31 app_user_region, V32 claim_fraud_confirmed.

---

# Performance Guidelines

Always filter by:

tenant_id

Avoid:

SELECT *

Use:

Pagination

Limit large result sets

Create indexes before production deployment.

---

# Domain-to-Table Mapping

**As-built (2026-07-29):** this mapping is aspirational and diverges heavily from what shipped. Notable deltas:
- **Access Control:** `role`, `permission`, `role_permission` exist; add `tenant_role_permission` (per-tenant overrides, V30).
- **User:** the table is `app_user` (not `user`); plus `user_session`, `user_invitation`, `user_branch_assignment`, and `app_user_region` (V31).
- **Policy/Product:** no `claim_type_policy`/`required_document_policy`/`assignment_policy`/`fraud_policy`/`sla_policy`/`analysis_strategy`. Instead: `insurance_product`, `insurance_product_version`, `product_document`, `insurance_policy`, `insured_vehicle`, and config-driven fraud lives in **`fraud_ruleset`/`fraud_rule`** (NOT `fraud_policy`).
- **Claim:** `claim` + append-only `claim_status_history` only. No `claim_history`/`claim_comment`/`claim_tag`/`claim_tag_mapping`.
- **Document:** `document`, `document_version`, `coverage_answer`/`coverage_citation`/`policy_chunk` (AI/RAG).
- **Assignment:** collapsed to `claim_assignment` (no `assignment_history`/`assignment_queue`).
- **Investigation:** collapsed to a single `investigation_note` (no `investigation`/`investigation_report`/`investigation_finding`/`investigation_evidence`).
- **Processing/OCR:** `claim_processing_state`, `ocr_job`/`analysis_job`/`fraud_job`, `ocr_result`/`analysis_result`. No `ocr_field_extraction`.
- **Fraud:** `fraud_score` only (append-only); no `fraud_alert`/`fraud_case`.
- **Notification:** single `notification` table; no `notification_template`/`notification_delivery`.
- **Analytics:** none built (no `analytics_snapshot`/`dashboard_metric`).
- **Audit:** `audit_log` only (append-only); no separate `audit_event`.

Organization Domain

* insurance_company
* region
* branch

Access Control Domain

* role
* permission
* role_permission

User Domain

* user
* customer
* user_branch_assignment

Policy Domain

* claim_type
* claim_type_policy
* required_document_policy
* assignment_policy
* fraud_policy
* fraud_rule
* sla_policy
* analysis_strategy

Claim Domain

* claim
* claim_history
* claim_comment
* claim_tag

Document Domain

* document
* document_version

Assignment Domain

* assignment
* assignment_history
* assignment_queue

Investigation Domain

* investigation
* investigation_note
* investigation_report

OCR Domain

* ocr_job
* ocr_result
* ocr_field_extraction

Fraud Domain

* fraud_score
* fraud_alert

Notification Domain

* notification
* notification_template

Analytics Domain

* analytics_snapshot
* dashboard_metric

Audit Domain

* audit_event
* audit_log

Processing Domain

* claim_processing_state



# Database Design Part 2 - Organization Domain

Status: Draft

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* insurance_company
* region
* branch

These tables form the organizational foundation of ClaimLens.

---

# Table: insurance_company

Purpose:

Represents a tenant in the platform.

Schema:

public

---

Columns

| Column              | Type         | Constraints     |
| ------------------- | ------------ | --------------- |
| id                  | BIGSERIAL    | PRIMARY KEY     |
| name                | VARCHAR(255) | NOT NULL        |
| code                | VARCHAR(50)  | NOT NULL UNIQUE |
| tenant_key          | VARCHAR(100) | NOT NULL UNIQUE |
| status              | VARCHAR(50)  | NOT NULL        |
| contact_email       | VARCHAR(255) |                 |
| contact_phone       | VARCHAR(50)  |                 |
| website             | VARCHAR(255) |                 |
| head_office_address | TEXT         |                 |
| logo_url            | TEXT         |                 |
| branding_config     | JSONB        |                 |
| subscription_plan   | VARCHAR(50)  | NOT NULL        |
| currency            | VARCHAR(10)  | NOT NULL        |
| timezone            | VARCHAR(100) | NOT NULL        |
| created_at          | TIMESTAMP    | NOT NULL        |
| created_by          | BIGINT       |                 |
| updated_at          | TIMESTAMP    |                 |
| updated_by          | BIGINT       |                 |
| is_deleted          | BOOLEAN      | DEFAULT FALSE   |
| deleted_at          | TIMESTAMP    |                 |
| deleted_by          | BIGINT       |                 |

---

Indexes

```sql
CREATE UNIQUE INDEX uq_insurance_company_code
ON insurance_company(code);

CREATE UNIQUE INDEX uq_insurance_company_tenant_key
ON insurance_company(tenant_key);

CREATE INDEX idx_insurance_company_status
ON insurance_company(status);
```

---

Constraints

Status Values:

```text
ACTIVE

ONBOARDING

SUSPENDED
```

Subscription Plans:

```text
BASIC

ENTERPRISE

CUSTOM
```

---

# Table: region

Purpose:

Represents a geographical operational area.

Examples:

```text
North Zone

South Zone

East Zone

West Zone
```

Schema:

public

---

Columns

| Column        | Type         | Constraints   |
| ------------- | ------------ | ------------- |
| id            | BIGSERIAL    | PRIMARY KEY   |
| tenant_id     | BIGINT       | NOT NULL      |
| code          | VARCHAR(50)  | NOT NULL      |
| name          | VARCHAR(255) | NOT NULL      |
| owner_user_id | BIGINT       |               |
| status        | VARCHAR(50)  | NOT NULL      |
| description   | TEXT         |               |
| created_at    | TIMESTAMP    | NOT NULL      |
| created_by    | BIGINT       |               |
| updated_at    | TIMESTAMP    |               |
| updated_by    | BIGINT       |               |
| is_deleted    | BOOLEAN      | DEFAULT FALSE |
| deleted_at    | TIMESTAMP    |               |
| deleted_by    | BIGINT       |               |

---

Foreign Keys

```sql
ALTER TABLE region
ADD CONSTRAINT fk_region_tenant
FOREIGN KEY (tenant_id)
REFERENCES insurance_company(id);
```

---

Unique Constraints

Region Code must be unique inside a tenant.

```sql
CREATE UNIQUE INDEX uq_region_tenant_code
ON region(tenant_id, code);
```

---

Indexes

```sql
CREATE INDEX idx_region_tenant
ON region(tenant_id);

CREATE INDEX idx_region_status
ON region(status);

CREATE INDEX idx_region_owner
ON region(owner_user_id);
```

---

Status Values

```text
ACTIVE

INACTIVE
```

---

Relationships

```text
InsuranceCompany
        1
        ↓
      Many
      Region
```

---

# Table: branch

Purpose:

Represents operational branch locations.

Examples:

```text
Delhi Branch

Noida Branch

Gurugram Branch
```

Schema:

public

---

Columns

| Column        | Type         | Constraints   |
| ------------- | ------------ | ------------- |
| id            | BIGSERIAL    | PRIMARY KEY   |
| tenant_id     | BIGINT       | NOT NULL      |
| region_id     | BIGINT       | NOT NULL      |
| code          | VARCHAR(50)  | NOT NULL      |
| name          | VARCHAR(255) | NOT NULL      |
| owner_user_id | BIGINT       |               |
| email         | VARCHAR(255) |               |
| phone         | VARCHAR(50)  |               |
| address       | TEXT         |               |
| city          | VARCHAR(100) |               |
| state         | VARCHAR(100) |               |
| country       | VARCHAR(100) |               |
| postal_code   | VARCHAR(20)  |               |
| status        | VARCHAR(50)  | NOT NULL      |
| description   | TEXT         |               |
| created_at    | TIMESTAMP    | NOT NULL      |
| created_by    | BIGINT       |               |
| updated_at    | TIMESTAMP    |               |
| updated_by    | BIGINT       |               |
| is_deleted    | BOOLEAN      | DEFAULT FALSE |
| deleted_at    | TIMESTAMP    |               |
| deleted_by    | BIGINT       |               |

---

Foreign Keys

```sql
ALTER TABLE branch
ADD CONSTRAINT fk_branch_tenant
FOREIGN KEY (tenant_id)
REFERENCES insurance_company(id);

ALTER TABLE branch
ADD CONSTRAINT fk_branch_region
FOREIGN KEY (region_id)
REFERENCES region(id);
```

---

Unique Constraints

Branch Code unique within tenant.

```sql
CREATE UNIQUE INDEX uq_branch_tenant_code
ON branch(tenant_id, code);
```

---

Indexes

```sql
CREATE INDEX idx_branch_tenant
ON branch(tenant_id);

CREATE INDEX idx_branch_region
ON branch(region_id);

CREATE INDEX idx_branch_status
ON branch(status);

CREATE INDEX idx_branch_owner
ON branch(owner_user_id);
```

---

Status Values

```text
ACTIVE

INACTIVE
```

---

Relationships

```text
InsuranceCompany
        ↓
      Region
        ↓
      Branch
```

---

# Organization Domain ERD

InsuranceCompany

```text
id (PK)
```

↓

Region

```text
id (PK)

tenant_id (FK)
```

↓

Branch

```text
id (PK)

tenant_id (FK)

region_id (FK)
```

---

# Query Optimization Strategy

Most common filters:

```sql
WHERE tenant_id = ?

WHERE status = ?

WHERE region_id = ?
```

Indexes have been added accordingly.

---

# Organization Domain Summary

Tables:

```text
insurance_company

region

branch
```

Primary Keys:

```text
BIGSERIAL
```

Tenant Isolation:

```text
tenant_id
```

Soft Delete:

```text
enabled
```

Audit Columns:

```text
enabled
```

Status Tracking:

```text
enabled
```



