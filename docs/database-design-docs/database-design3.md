# Database Design Part 5 - Policy Engine Domain

Status: Draft

Version: 1.0

Owner: RiskLens Technologies

---

# Purpose

This section defines the physical PostgreSQL design for the Policy Engine.

The Policy Engine drives:

* Claim Processing
* Required Documents
* Assignment Rules
* Fraud Rules
* SLA Rules
* Analysis Behavior

The goal is to allow Company Admins to modify business behavior without code changes.

---

# Policy Hierarchy

Claim Type

↓

Claim Type Policy

↓

Required Document Policy

Assignment Policy

Fraud Policy

SLA Policy

Analysis Strategy

---

# Table: claim_type

Purpose:

Defines supported insurance claim categories.

Schema:

public

---

Columns

| Column      | Type         | Constraints   |
| ----------- | ------------ | ------------- |
| id          | BIGSERIAL    | PRIMARY KEY   |
| tenant_id   | BIGINT       | NOT NULL      |
| code        | VARCHAR(50)  | NOT NULL      |
| name        | VARCHAR(100) | NOT NULL      |
| description | TEXT         |               |
| is_enabled  | BOOLEAN      | DEFAULT TRUE  |
| created_at  | TIMESTAMP    | NOT NULL      |
| created_by  | BIGINT       |               |
| updated_at  | TIMESTAMP    |               |
| updated_by  | BIGINT       |               |
| is_deleted  | BOOLEAN      | DEFAULT FALSE |
| deleted_at  | TIMESTAMP    |               |
| deleted_by  | BIGINT       |               |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_claim_type_tenant_code
ON claim_type(tenant_id, code);
```

---

Indexes

```sql
CREATE INDEX idx_claim_type_tenant
ON claim_type(tenant_id);

CREATE INDEX idx_claim_type_enabled
ON claim_type(is_enabled);
```

---

V1 Supported Types

```text
MOTOR
```

Future:

```text
HEALTH
PROPERTY
TRAVEL
LIFE
```

---

# Table: claim_type_policy

Purpose:

Root policy container.

All processing behavior is attached to a policy version.

Schema:

public

---

Columns

| Column         | Type         | Constraints |
| -------------- | ------------ | ----------- |
| id             | BIGSERIAL    | PRIMARY KEY |
| tenant_id      | BIGINT       | NOT NULL    |
| claim_type_id  | BIGINT       | NOT NULL    |
| name           | VARCHAR(255) | NOT NULL    |
| version        | INTEGER      | NOT NULL    |
| status         | VARCHAR(50)  | NOT NULL    |
| effective_from | TIMESTAMP    |             |
| effective_to   | TIMESTAMP    |             |
| created_at     | TIMESTAMP    | NOT NULL    |
| created_by     | BIGINT       |             |
| updated_at     | TIMESTAMP    |             |
| updated_by     | BIGINT       |             |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)

claim_type_id
    → claim_type(id)
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_claim_policy_version
ON claim_type_policy(tenant_id, claim_type_id, version);
```

---

Indexes

```sql
CREATE INDEX idx_claim_policy_status
ON claim_type_policy(status);
```

---

Status Values

```text
DRAFT

ACTIVE

ARCHIVED
```

---

# Table: document_type

Purpose:

Master document catalog.

Schema:

public

---

Columns

| Column      | Type         | Constraints |
| ----------- | ------------ | ----------- |
| id          | BIGSERIAL    | PRIMARY KEY |
| tenant_id   | BIGINT       | NOT NULL    |
| code        | VARCHAR(100) | NOT NULL    |
| name        | VARCHAR(255) | NOT NULL    |
| description | TEXT         |             |
| status      | VARCHAR(50)  | NOT NULL    |
| created_at  | TIMESTAMP    | NOT NULL    |

---

Examples

```text
ACCIDENT_PHOTO

POLICE_REPORT

REPAIR_ESTIMATE

VEHICLE_RC

DRIVING_LICENSE
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_document_type_code
ON document_type(tenant_id, code);
```

---

# Table: required_document_policy

Purpose:

Defines required documents per claim type.

Schema:

public

---

Columns

| Column               | Type      | Constraints |
| -------------------- | --------- | ----------- |
| id                   | BIGSERIAL | PRIMARY KEY |
| claim_type_policy_id | BIGINT    | NOT NULL    |
| document_type_id     | BIGINT    | NOT NULL    |
| is_required          | BOOLEAN   | NOT NULL    |
| min_documents        | INTEGER   | DEFAULT 1   |
| max_documents        | INTEGER   | DEFAULT 1   |
| created_at           | TIMESTAMP | NOT NULL    |

---

Foreign Keys

```sql
claim_type_policy_id
    → claim_type_policy(id)

document_type_id
    → document_type(id)
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_required_document_policy
ON required_document_policy(
    claim_type_policy_id,
    document_type_id
);
```

---

# Table: assignment_policy

Purpose:

Controls investigator assignment behavior.

Schema:

public

---

Columns

| Column                             | Type        | Constraints   |
| ---------------------------------- | ----------- | ------------- |
| id                                 | BIGSERIAL   | PRIMARY KEY   |
| claim_type_policy_id               | BIGINT      | NOT NULL      |
| auto_assignment_enabled            | BOOLEAN     | DEFAULT TRUE  |
| assignment_strategy                | VARCHAR(50) | NOT NULL      |
| max_active_claims_per_investigator | INTEGER     |               |
| allow_cross_branch_assignment      | BOOLEAN     | DEFAULT TRUE  |
| allow_cross_region_assignment      | BOOLEAN     | DEFAULT FALSE |
| created_at                         | TIMESTAMP   | NOT NULL      |

---

Strategies

```text
ROUND_ROBIN

LEAST_LOADED

MANUAL
```

---

Foreign Keys

```sql
claim_type_policy_id
    → claim_type_policy(id)
```

---

# Table: fraud_policy

Purpose:

Controls fraud scoring thresholds.

Schema:

public

---

Columns

| Column                    | Type      | Constraints |
| ------------------------- | --------- | ----------- |
| id                        | BIGSERIAL | PRIMARY KEY |
| claim_type_policy_id      | BIGINT    | NOT NULL    |
| medium_risk_threshold     | INTEGER   | NOT NULL    |
| high_risk_threshold       | INTEGER   | NOT NULL    |
| auto_escalation_threshold | INTEGER   | NOT NULL    |
| created_at                | TIMESTAMP | NOT NULL    |

---

Example

```text
MEDIUM = 40

HIGH = 70

ESCALATE = 85
```

---

Foreign Keys

```sql
claim_type_policy_id
    → claim_type_policy(id)
```

---

# Table: fraud_rule

Purpose:

Defines configurable fraud checks.

Schema:

public

---

Columns

| Column          | Type         | Constraints  |
| --------------- | ------------ | ------------ |
| id              | BIGSERIAL    | PRIMARY KEY  |
| fraud_policy_id | BIGINT       | NOT NULL     |
| rule_code       | VARCHAR(100) | NOT NULL     |
| rule_name       | VARCHAR(255) | NOT NULL     |
| weight          | INTEGER      | NOT NULL     |
| rule_parameters | JSONB        |              |
| is_enabled      | BOOLEAN      | DEFAULT TRUE |
| created_at      | TIMESTAMP    | NOT NULL     |

---

Examples

```text
DUPLICATE_IMAGE

DUPLICATE_CLAIM

LOCATION_MISMATCH

MISSING_DOCUMENT

REPAIR_COST_ANOMALY
```

---

Example JSON

```json
{
  "allowedDeviationPercentage": 30
}
```

---

Foreign Keys

```sql
fraud_policy_id
    → fraud_policy(id)
```

---

Indexes

```sql
CREATE INDEX idx_fraud_rule_enabled
ON fraud_rule(is_enabled);

CREATE INDEX idx_fraud_rule_code
ON fraud_rule(rule_code);
```

---

# Table: sla_policy

Purpose:

Defines claim processing deadlines.

Schema:

public

---

Columns

| Column                | Type        | Constraints |
| --------------------- | ----------- | ----------- |
| id                    | BIGSERIAL   | PRIMARY KEY |
| claim_type_policy_id  | BIGINT      | NOT NULL    |
| risk_level            | VARCHAR(50) | NOT NULL    |
| response_time_hours   | INTEGER     | NOT NULL    |
| resolution_time_hours | INTEGER     | NOT NULL    |
| created_at            | TIMESTAMP   | NOT NULL    |

---

Risk Levels

```text
LOW

MEDIUM

HIGH
```

---

Foreign Keys

```sql
claim_type_policy_id
    → claim_type_policy(id)
```

---

# Table: analysis_strategy

Purpose:

Controls processing pipeline behavior.

Schema:

public

---

Columns

| Column                 | Type      | Constraints   |
| ---------------------- | --------- | ------------- |
| id                     | BIGSERIAL | PRIMARY KEY   |
| claim_type_policy_id   | BIGINT    | NOT NULL      |
| ocr_enabled            | BOOLEAN   | DEFAULT TRUE  |
| image_analysis_enabled | BOOLEAN   | DEFAULT TRUE  |
| fraud_analysis_enabled | BOOLEAN   | DEFAULT TRUE  |
| ml_scoring_enabled     | BOOLEAN   | DEFAULT FALSE |
| manual_review_required | BOOLEAN   | DEFAULT FALSE |
| created_at             | TIMESTAMP | NOT NULL      |

---

Foreign Keys

```sql
claim_type_policy_id
    → claim_type_policy(id)
```

---

# Policy Engine ERD

ClaimType

↓

ClaimTypePolicy

↓

RequiredDocumentPolicy

↓

DocumentType

---

ClaimTypePolicy

↓

AssignmentPolicy

---

ClaimTypePolicy

↓

FraudPolicy

↓

FraudRule

---

ClaimTypePolicy

↓

SLAPolicy

---

ClaimTypePolicy

↓

AnalysisStrategy

---

# Query Optimization Strategy

Common Queries

```sql
WHERE claim_type_policy_id = ?

WHERE fraud_policy_id = ?

WHERE is_enabled = TRUE

WHERE status = 'ACTIVE'
```

Indexes support these patterns.

---

# Policy Engine Summary

Tables

```text
claim_type

claim_type_policy

document_type

required_document_policy

assignment_policy

fraud_policy

fraud_rule

sla_policy

analysis_strategy
```

Configuration Driven

```text
YES
```

Versioned Policies

```text
YES
```

Admin Managed

```text
YES
```

Code Changes Required

```text
NO
```







# Database Design Part 6 - Claim Domain

Status: Draft

Version: 1.0

Owner: RiskLens Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* claim
* claim_history
* claim_comment
* claim_tag
* claim_tag_mapping

The Claim Domain is the core business domain of ClaimLens.

Every investigation, assignment, fraud analysis, OCR process, notification, and audit event originates from a claim.

---

# Claim Design Principles

A Claim:

* Belongs to exactly one Tenant
* Belongs to exactly one Customer
* Belongs to exactly one Claim Type
* Has exactly one active policy version
* Has one vehicle (V1)
* Can have multiple documents
* Can have multiple assignments over time
* Can have multiple fraud alerts
* Can have one active investigation

---

# Table: claim

Purpose:

Represents an insurance claim submitted for investigation.

Schema:

public

---

Columns

| Column                   | Type          | Constraints   |
| ------------------------ | ------------- | ------------- |
| id                       | BIGSERIAL     | PRIMARY KEY   |
| tenant_id                | BIGINT        | NOT NULL      |
| customer_id              | BIGINT        | NOT NULL      |
| claim_type_id            | BIGINT        | NOT NULL      |
| claim_type_policy_id     | BIGINT        | NOT NULL      |
| claim_number             | VARCHAR(100)  | NOT NULL      |
| external_claim_reference | VARCHAR(255)  |               |
| title                    | VARCHAR(255)  | NOT NULL      |
| description              | TEXT          |               |
| incident_date            | TIMESTAMP     | NOT NULL      |
| reported_date            | TIMESTAMP     | NOT NULL      |
| claim_amount             | NUMERIC(18,2) |               |
| approved_amount          | NUMERIC(18,2) |               |
| fraud_score              | INTEGER       |               |
| fraud_risk_level         | VARCHAR(50)   |               |
| current_status           | VARCHAR(50)   | NOT NULL      |
| current_assignment_id    | BIGINT        |               |
| current_investigation_id | BIGINT        |               |
| assigned_region_id       | BIGINT        |               |
| assigned_branch_id       | BIGINT        |               |
| submitted_at             | TIMESTAMP     |               |
| closed_at                | TIMESTAMP     |               |
| closure_reason           | TEXT          |               |
| created_at               | TIMESTAMP     | NOT NULL      |
| created_by               | BIGINT        |               |
| updated_at               | TIMESTAMP     |               |
| updated_by               | BIGINT        |               |
| is_deleted               | BOOLEAN       | DEFAULT FALSE |
| deleted_at               | TIMESTAMP     |               |
| deleted_by               | BIGINT        |               |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)

customer_id
    → customer(id)

claim_type_id
    → claim_type(id)

claim_type_policy_id
    → claim_type_policy(id)

assigned_region_id
    → region(id)

assigned_branch_id
    → branch(id)
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_claim_tenant_claim_number
ON claim(tenant_id, claim_number);
```

---

Claim Number Format

Examples:

```text
CLM-2026-000001

CLM-2026-000002

CLM-2026-000003
```

---

Claim Status Values

```text
DRAFT

SUBMITTED

AWAITING_ANALYSIS

AWAITING_ASSIGNMENT

AWAITING_ACCEPTANCE

UNDER_INVESTIGATION

WAITING_FOR_CUSTOMER

APPROVED

REJECTED

CLOSED

CANCELLED
```

---

Fraud Risk Levels

```text
LOW

MEDIUM

HIGH
```

---

Indexes

```sql
CREATE INDEX idx_claim_tenant
ON claim(tenant_id);

CREATE INDEX idx_claim_customer
ON claim(customer_id);

CREATE INDEX idx_claim_status
ON claim(current_status);

CREATE INDEX idx_claim_type
ON claim(claim_type_id);

CREATE INDEX idx_claim_incident_date
ON claim(incident_date);

CREATE INDEX idx_claim_created_at
ON claim(created_at);

CREATE INDEX idx_claim_tenant_status
ON claim(tenant_id, current_status);
```

---

# Motor Claim Fields (V1)

Because V1 only supports:

```text
MOTOR CLAIMS
```

The claim table contains vehicle fields directly.

---

Additional Columns

| Column                      | Type         |
| --------------------------- | ------------ |
| vehicle_registration_number | VARCHAR(100) |
| vehicle_make                | VARCHAR(100) |
| vehicle_model               | VARCHAR(100) |
| vehicle_year                | INTEGER      |
| driver_name                 | VARCHAR(255) |
| driver_license_number       | VARCHAR(255) |

---

Future

When Health, Property, Travel claims are introduced:

```text
claim
    ↓
claim_type_specific tables
```

---

# Table: claim_history

Purpose:

Stores immutable status transitions.

Every major claim lifecycle event creates a history record.

Schema:

public

---

Columns

| Column          | Type        | Constraints |
| --------------- | ----------- | ----------- |
| id              | BIGSERIAL   | PRIMARY KEY |
| tenant_id       | BIGINT      | NOT NULL    |
| claim_id        | BIGINT      | NOT NULL    |
| previous_status | VARCHAR(50) |             |
| new_status      | VARCHAR(50) | NOT NULL    |
| change_reason   | TEXT        |             |
| changed_by      | BIGINT      |             |
| created_at      | TIMESTAMP   | NOT NULL    |

---

Foreign Keys

```sql
claim_id
    → claim(id)

tenant_id
    → insurance_company(id)
```

---

Indexes

```sql
CREATE INDEX idx_claim_history_claim
ON claim_history(claim_id);

CREATE INDEX idx_claim_history_created_at
ON claim_history(created_at);
```

---

Example

```text
SUBMITTED
↓
AWAITING_ANALYSIS

AWAITING_ANALYSIS
↓
UNDER_INVESTIGATION

UNDER_INVESTIGATION
↓
APPROVED
```

---

# Table: claim_comment

Purpose:

Stores communication and investigation notes attached to a claim.

Schema:

public

---

Columns

| Column         | Type      | Constraints  |
| -------------- | --------- | ------------ |
| id             | BIGSERIAL | PRIMARY KEY  |
| tenant_id      | BIGINT    | NOT NULL     |
| claim_id       | BIGINT    | NOT NULL     |
| author_user_id | BIGINT    | NOT NULL     |
| comment_text   | TEXT      | NOT NULL     |
| is_internal    | BOOLEAN   | DEFAULT TRUE |
| created_at     | TIMESTAMP | NOT NULL     |

---

Foreign Keys

```sql
claim_id
    → claim(id)

author_user_id
    → user(id)
```

---

Indexes

```sql
CREATE INDEX idx_claim_comment_claim
ON claim_comment(claim_id);

CREATE INDEX idx_claim_comment_author
ON claim_comment(author_user_id);
```

---

Comment Types

Internal:

```text
Investigator Notes

Manager Notes

Fraud Review Notes
```

Future:

External Customer Comments.

---

# Table: claim_tag

Purpose:

Master tag catalog.

Used for categorization and analytics.

Schema:

public

---

Columns

| Column      | Type         | Constraints |
| ----------- | ------------ | ----------- |
| id          | BIGSERIAL    | PRIMARY KEY |
| tenant_id   | BIGINT       | NOT NULL    |
| code        | VARCHAR(100) | NOT NULL    |
| name        | VARCHAR(255) | NOT NULL    |
| description | TEXT         |             |
| created_at  | TIMESTAMP    | NOT NULL    |

---

Examples

```text
HIGH_PRIORITY

FRAUD_REVIEW

VIP_CUSTOMER

LEGAL_REVIEW

ESCALATED
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_claim_tag_code
ON claim_tag(tenant_id, code);
```

---

# Table: claim_tag_mapping

Purpose:

Many-to-many relationship between claims and tags.

Schema:

public

---

Columns

| Column     | Type      | Constraints |
| ---------- | --------- | ----------- |
| id         | BIGSERIAL | PRIMARY KEY |
| claim_id   | BIGINT    | NOT NULL    |
| tag_id     | BIGINT    | NOT NULL    |
| created_at | TIMESTAMP | NOT NULL    |
| created_by | BIGINT    |             |

---

Foreign Keys

```sql
claim_id
    → claim(id)

tag_id
    → claim_tag(id)
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_claim_tag_mapping
ON claim_tag_mapping(claim_id, tag_id);
```

---

Indexes

```sql
CREATE INDEX idx_claim_tag_mapping_claim
ON claim_tag_mapping(claim_id);

CREATE INDEX idx_claim_tag_mapping_tag
ON claim_tag_mapping(tag_id);
```

---

# Claim Lifecycle Model

Draft

↓

Submitted

↓

Awaiting Analysis

↓

Awaiting Assignment

↓

Awaiting Acceptance

↓

Under Investigation

↓

Waiting For Customer

↓

Approved / Rejected

↓

Closed

---

# Claim Domain ERD

Customer

↓

Claim

↓

ClaimHistory

↓

ClaimComment

---

Claim

↓

ClaimTagMapping

↓

ClaimTag

---

# Query Optimization Strategy

Most Common Queries

```sql
WHERE tenant_id = ?

WHERE current_status = ?

WHERE assigned_branch_id = ?

WHERE assigned_region_id = ?

WHERE fraud_risk_level = ?

WHERE created_at >= ?
```

Indexes added accordingly.

---

# Claim Domain Summary

Tables

```text
claim

claim_history

claim_comment

claim_tag

claim_tag_mapping
```

Features

```text
Versioned Status Tracking      YES

Comments & Collaboration       YES

Fraud Risk Tracking            YES

Claim Tagging                  YES

Motor Claim Support            YES

Multi-Tenant Isolation         YES
```

Core Business Entity

```text
claim
```
