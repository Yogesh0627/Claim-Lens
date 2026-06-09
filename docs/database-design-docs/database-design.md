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



