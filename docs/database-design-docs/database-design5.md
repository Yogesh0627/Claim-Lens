> **⚠️ Design-era document — reconciled against the as-built schema on 2026-07-29.** This is an iterative pre-implementation draft. The authoritative schema is the **Flyway migrations `V1__…V32__`** (backend/src/main/resources/db/migration) and [`../domain-model.md`](../domain-model.md). Where this draft diverges, the migrations win; key deltas are flagged inline as **As-built** notes.

# Database Design Part 9 - Investigation Domain

**As-built (2026-07-29):** the entire investigation domain was **collapsed to a single `investigation_note` table** (V16). There is NO `investigation`, `investigation_report`, or `investigation_task` table (and no `investigation_finding`/`investigation_evidence`). `investigation_note` carries `claim_id` directly (not an `investigation_id`), plus `note_type` (FRAUD_OBSERVATION, SITE_VISIT, CUSTOMER_INTERACTION, MANAGER_REVIEW, ESCALATION, GENERAL), `note`, an optional `severity` (LOW/MEDIUM/HIGH), and an optional `document_id` FK. Investigation lifecycle state is tracked by the claim's `status` + `claim_status_history`, not a dedicated investigation row.

Status: Draft

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* investigation
* investigation_note
* investigation_report

The Investigation Domain manages the entire claim investigation lifecycle after assignment acceptance.

This domain is responsible for:

* Evidence Collection
* Investigator Findings
* Internal Collaboration
* Investigation Reports
* Claim Recommendations
* Claim Closure Decisions

---

# Investigation Design Principles

A Claim can have:

```text
Multiple Investigations
```

Examples:

```text
Investigation #1
    ↓
Investigator Resigned

Investigation #2
    ↓
Completed
```

However:

```text
Only One Active Investigation
```

may exist at any given time.

---

# Investigation Lifecycle

Assignment Accepted

↓

Investigation Created

↓

Evidence Review

↓

Document Review

↓

Fraud Review

↓

Additional Information Request (Optional)

↓

Findings Recorded

↓

Report Submitted

↓

Recommendation Generated

↓

Claim Approved / Rejected

↓

Investigation Closed

---

# Table: investigation

Purpose:

Represents an active investigation for a claim.

Schema:

public

---

Columns

| Column               | Type         | Constraints |
| -------------------- | ------------ | ----------- |
| id                   | BIGSERIAL    | PRIMARY KEY |
| tenant_id            | BIGINT       | NOT NULL    |
| claim_id             | BIGINT       | NOT NULL    |
| assignment_id        | BIGINT       | NOT NULL    |
| investigator_id      | BIGINT       | NOT NULL    |
| manager_id           | BIGINT       |             |
| investigation_number | VARCHAR(100) | NOT NULL    |
| status               | VARCHAR(50)  | NOT NULL    |
| priority             | VARCHAR(50)  | NOT NULL    |
| started_at           | TIMESTAMP    | NOT NULL    |
| due_at               | TIMESTAMP    |             |
| completed_at         | TIMESTAMP    |             |
| recommendation       | VARCHAR(50)  |             |
| summary              | TEXT         |             |
| created_at           | TIMESTAMP    | NOT NULL    |
| created_by           | BIGINT       |             |
| updated_at           | TIMESTAMP    |             |
| updated_by           | BIGINT       |             |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)

claim_id
    → claim(id)

assignment_id
    → assignment(id)

investigator_id
    → user(id)

manager_id
    → user(id)
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_investigation_number
ON investigation(investigation_number);
```

---

Investigation Number Format

Examples

```text
INV-2026-000001

INV-2026-000002
```

---

Status Values

```text
OPEN

IN_PROGRESS

WAITING_FOR_CUSTOMER

UNDER_MANAGER_REVIEW

COMPLETED

CANCELLED
```

---

Priority Values

```text
LOW

MEDIUM

HIGH

CRITICAL
```

---

Recommendation Values

```text
APPROVE

REJECT

NEED_MORE_INFORMATION

ESCALATE
```

---

Indexes

```sql
CREATE INDEX idx_investigation_tenant
ON investigation(tenant_id);

CREATE INDEX idx_investigation_claim
ON investigation(claim_id);

CREATE INDEX idx_investigation_investigator
ON investigation(investigator_id);

CREATE INDEX idx_investigation_status
ON investigation(status);

CREATE INDEX idx_investigation_due_at
ON investigation(due_at);
```

---

Business Rule

Only one active investigation per claim.

```sql
CREATE UNIQUE INDEX uq_claim_active_investigation
ON investigation(claim_id)
WHERE status IN (
    'OPEN',
    'IN_PROGRESS',
    'WAITING_FOR_CUSTOMER',
    'UNDER_MANAGER_REVIEW'
);
```

---

# Table: investigation_note

Purpose:

Stores investigator and manager notes.

Schema:

public

---

Columns

| Column           | Type        | Constraints  |
| ---------------- | ----------- | ------------ |
| id               | BIGSERIAL   | PRIMARY KEY  |
| tenant_id        | BIGINT      | NOT NULL     |
| investigation_id | BIGINT      | NOT NULL     |
| author_user_id   | BIGINT      | NOT NULL     |
| note_type        | VARCHAR(50) | NOT NULL     |
| note_text        | TEXT        | NOT NULL     |
| is_private       | BOOLEAN     | DEFAULT TRUE |
| created_at       | TIMESTAMP   | NOT NULL     |

---

Foreign Keys

```sql
investigation_id
    → investigation(id)

author_user_id
    → user(id)
```

---

Note Types

```text
GENERAL_NOTE

FRAUD_OBSERVATION

CUSTOMER_INTERACTION

SITE_VISIT

MANAGER_REVIEW

ESCALATION_NOTE
```

---

Indexes

```sql
CREATE INDEX idx_investigation_note_investigation
ON investigation_note(investigation_id);

CREATE INDEX idx_investigation_note_author
ON investigation_note(author_user_id);

CREATE INDEX idx_investigation_note_created_at
ON investigation_note(created_at);
```

---

Examples

```text
Vehicle damage appears inconsistent with reported accident.

Customer contacted on 15 June.

Repair estimate significantly exceeds market average.
```

---

# Table: investigation_task

Purpose:

Tracks investigation activities and delegated work.

Schema:

public

---

Columns

| Column              | Type         | Constraints |
| ------------------- | ------------ | ----------- |
| id                  | BIGSERIAL    | PRIMARY KEY |
| tenant_id           | BIGINT       | NOT NULL    |
| investigation_id    | BIGINT       | NOT NULL    |
| assigned_to_user_id | BIGINT       | NOT NULL    |
| title               | VARCHAR(255) | NOT NULL    |
| description         | TEXT         |             |
| status              | VARCHAR(50)  | NOT NULL    |
| due_at              | TIMESTAMP    |             |
| completed_at        | TIMESTAMP    |             |
| created_at          | TIMESTAMP    | NOT NULL    |
| created_by          | BIGINT       | NOT NULL    |

---

Foreign Keys

```sql
investigation_id
    → investigation(id)

assigned_to_user_id
    → user(id)
```

---

Status Values

```text
OPEN

IN_PROGRESS

COMPLETED

CANCELLED
```

---

Indexes

```sql
CREATE INDEX idx_investigation_task_assigned
ON investigation_task(assigned_to_user_id);

CREATE INDEX idx_investigation_task_status
ON investigation_task(status);
```

---

# Table: investigation_report

Purpose:

Represents the final investigation outcome.

Only one final report is allowed per investigation.

Schema:

public

---

Columns

| Column             | Type          | Constraints |
| ------------------ | ------------- | ----------- |
| id                 | BIGSERIAL     | PRIMARY KEY |
| tenant_id          | BIGINT        | NOT NULL    |
| investigation_id   | BIGINT        | NOT NULL    |
| report_number      | VARCHAR(100)  | NOT NULL    |
| executive_summary  | TEXT          | NOT NULL    |
| findings           | TEXT          | NOT NULL    |
| fraud_indicators   | TEXT          |             |
| recommendation     | VARCHAR(50)   | NOT NULL    |
| recommended_amount | NUMERIC(18,2) |             |
| submitted_at       | TIMESTAMP     | NOT NULL    |
| submitted_by       | BIGINT        | NOT NULL    |
| approved_at        | TIMESTAMP     |             |
| approved_by        | BIGINT        |             |
| created_at         | TIMESTAMP     | NOT NULL    |

---

Foreign Keys

```sql
investigation_id
    → investigation(id)

submitted_by
    → user(id)

approved_by
    → user(id)
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_investigation_report
ON investigation_report(investigation_id);
```

---

Recommendation Values

```text
APPROVE

REJECT

NEED_MORE_INFORMATION

ESCALATE
```

---

Indexes

```sql
CREATE INDEX idx_investigation_report_submitted_at
ON investigation_report(submitted_at);
```

---

# Investigation Closure Flow

Investigation

↓

Report Submitted

↓

Manager Review

↓

Recommendation Accepted

↓

Claim Updated

↓

Investigation Completed

↓

Assignment Completed

---

# Additional Information Flow

Investigator

↓

Request Documents

↓

Claim Status

WAITING_FOR_CUSTOMER

↓

Customer Uploads New Version

↓

Document Reprocessing

↓

Investigation Resumes

---

# Investigation Domain ERD

Claim

↓

Investigation

↓

InvestigationNote

---

Investigation

↓

InvestigationTask

---

Investigation

↓

InvestigationReport

---

Assignment

↓

Investigation

---

# Query Optimization Strategy

Most Common Queries

```sql
WHERE investigator_id = ?

WHERE claim_id = ?

WHERE status = ?

WHERE due_at <= NOW()

WHERE recommendation = ?
```

Indexes added accordingly.

---

# Investigation Domain Summary

Tables

```text
investigation

investigation_note

investigation_task

investigation_report
```

Supports

```text
Investigator Workflow          YES

Manager Review                YES

Task Delegation               YES

Evidence Notes                YES

Final Reports                 YES

Recommendations               YES

Additional Info Requests      YES

Full Audit Trail              YES
```

Core Entity

```text
investigation
```

Investigation Engine Ready

```text
YES
```





# Database Design Part 10 - OCR & Processing Domain

**As-built (2026-07-29):** these tables live in the single **`public` schema**, NOT a separate `processing` schema (drop the `processing.` prefix everywhere below). Built: `claim_processing_state` (V14), the job tables `ocr_job`/`analysis_job`/`fraud_job` (V14; there is also a `fraud_job` gate not shown here), `ocr_result` (V20), `analysis_result` (V21). **`ocr_field_extraction` was NOT built** — extracted text/fields live inside `ocr_result`. OCR itself is pluggable (Google Vision / Python service / NoOp) and image analysis runs against a Python FastAPI service (or NoOp).

Status: Draft

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* claim_processing_state
* ocr_job
* analysis_job
* ocr_result
* ocr_field_extraction
* analysis_result

This domain powers:

* OCR Processing
* Image Analysis
* Incremental Reprocessing
* Worker Recovery
* Fraud Orchestration

It is the backbone of the Claim Processing Orchestrator.

---

# Processing Design Principles

Heavy processing is:

```text
Asynchronous
```

Never executed during request-response cycles.

All processing happens through:

```text
Job Tables
↓
Workers
↓
Processing Results
```

---

# Processing Pipeline

Claim Submitted

↓

OCR Job

↓

Analysis Job

↓

Claim Snapshot Builder

↓

Fraud Analysis

↓

Assignment

---

# Table: claim_processing_state

Purpose:

Tracks overall processing progress of a claim.

Used by:

* Claim Processing Orchestrator
* Fraud Trigger Logic
* Reprocessing Logic

Schema:

processing

---

Columns

| Column                             | Type        | Constraints   |
| ---------------------------------- | ----------- | ------------- |
| id                                 | BIGSERIAL   | PRIMARY KEY   |
| tenant_id                          | BIGINT      | NOT NULL      |
| claim_id                           | BIGINT      | NOT NULL      |
| ocr_status                         | VARCHAR(50) | NOT NULL      |
| analysis_status                    | VARCHAR(50) | NOT NULL      |
| fraud_status                       | VARCHAR(50) | NOT NULL      |
| pending_reprocess                  | BOOLEAN     | DEFAULT FALSE |
| last_processed_document_version_id | BIGINT      |               |
| last_updated_at                    | TIMESTAMP   | NOT NULL      |
| created_at                         | TIMESTAMP   | NOT NULL      |

---

Foreign Keys

```sql
claim_id
    → claim(id)
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_claim_processing_state
ON claim_processing_state(claim_id);
```

---

Status Values

```text
PENDING

PROCESSING

COMPLETE

FAILED

NOT_STARTED

QUEUED
```

---

Indexes

```sql
CREATE INDEX idx_claim_processing_claim
ON claim_processing_state(claim_id);
```

---

# Table: ocr_job

Purpose:

Tracks OCR work assigned to OCR workers.

Schema:

processing

---

Columns

| Column              | Type         |
| ------------------- | ------------ |
| id                  | BIGSERIAL    |
| tenant_id           | BIGINT       |
| claim_id            | BIGINT       |
| document_version_id | BIGINT       |
| status              | VARCHAR(50)  |
| attempt_count       | INTEGER      |
| locked_by           | VARCHAR(255) |
| locked_at           | TIMESTAMP    |
| started_at          | TIMESTAMP    |
| completed_at        | TIMESTAMP    |
| failure_reason      | TEXT         |
| created_at          | TIMESTAMP    |
| updated_at          | TIMESTAMP    |

---

Foreign Keys

```sql
claim_id
    → claim(id)

document_version_id
    → document_version(id)
```

---

Status Values

```text
PENDING

PROCESSING

COMPLETE

FAILED
```

---

Indexes

```sql
CREATE INDEX idx_ocr_job_status
ON processing.ocr_job(status);

CREATE INDEX idx_ocr_job_claim
ON processing.ocr_job(claim_id);

CREATE INDEX idx_ocr_job_document_version
ON processing.ocr_job(document_version_id);
```

---

Worker Claim Strategy

```sql
SELECT *
FROM processing.ocr_job
WHERE status = 'PENDING'
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

---

# Table: analysis_job

Purpose:

Tracks image analysis work.

Schema:

processing

---

Columns

| Column              | Type         |
| ------------------- | ------------ |
| id                  | BIGSERIAL    |
| tenant_id           | BIGINT       |
| claim_id            | BIGINT       |
| document_version_id | BIGINT       |
| status              | VARCHAR(50)  |
| attempt_count       | INTEGER      |
| locked_by           | VARCHAR(255) |
| locked_at           | TIMESTAMP    |
| started_at          | TIMESTAMP    |
| completed_at        | TIMESTAMP    |
| failure_reason      | TEXT         |
| created_at          | TIMESTAMP    |
| updated_at          | TIMESTAMP    |

---

Foreign Keys

```sql
claim_id
    → claim(id)

document_version_id
    → document_version(id)
```

---

Indexes

```sql
CREATE INDEX idx_analysis_job_status
ON processing.analysis_job(status);

CREATE INDEX idx_analysis_job_claim
ON processing.analysis_job(claim_id);
```

---

# Worker Recovery Strategy

Purpose:

Recover abandoned jobs.

Query:

```sql
SELECT *
FROM processing.ocr_job
WHERE status = 'PROCESSING'
AND locked_at < NOW() - INTERVAL '10 minutes';
```

↓

Reset

```text
PENDING
```

↓

Retry

---

# Table: ocr_result

Purpose:

Stores OCR metadata.

Raw OCR payloads are stored in S3.

Schema:

processing

---

Columns

| Column              | Type         |
| ------------------- | ------------ |
| id                  | BIGSERIAL    |
| tenant_id           | BIGINT       |
| claim_id            | BIGINT       |
| document_version_id | BIGINT       |
| payload_location    | TEXT         |
| confidence_score    | NUMERIC(5,2) |
| language_detected   | VARCHAR(100) |
| processing_time_ms  | BIGINT       |
| created_at          | TIMESTAMP    |

---

Foreign Keys

```sql
claim_id
    → claim(id)

document_version_id
    → document_version(id)
```

---

Example Payload Location

```text
s3://claimlens/tenant-1001/ocr/5001/doc-9001-v2.json
```

---

Indexes

```sql
CREATE INDEX idx_ocr_result_document_version
ON processing.ocr_result(document_version_id);
```

---

# Table: ocr_field_extraction

Purpose:

Stores searchable OCR fields.

Schema:

processing

---

Columns

| Column           | Type         |
| ---------------- | ------------ |
| id               | BIGSERIAL    |
| tenant_id        | BIGINT       |
| ocr_result_id    | BIGINT       |
| field_name       | VARCHAR(255) |
| field_value      | TEXT         |
| confidence_score | NUMERIC(5,2) |
| created_at       | TIMESTAMP    |

---

Foreign Keys

```sql
ocr_result_id
    → processing.ocr_result(id)
```

---

Examples

```text
Policy Number

Vehicle Number

License Number

Repair Cost

Customer Name
```

---

Indexes

```sql
CREATE INDEX idx_ocr_field_name
ON processing.ocr_field_extraction(field_name);
```

---

# Table: analysis_result

Purpose:

Stores image analysis results.

Schema:

processing

---

Columns

| Column              | Type         |
| ------------------- | ------------ |
| id                  | BIGSERIAL    |
| tenant_id           | BIGINT       |
| claim_id            | BIGINT       |
| document_version_id | BIGINT       |
| duplicate_detected  | BOOLEAN      |
| similarity_score    | NUMERIC(5,2) |
| image_hash          | VARCHAR(255) |
| metadata_json       | JSONB        |
| processing_time_ms  | BIGINT       |
| created_at          | TIMESTAMP    |

---

Foreign Keys

```sql
claim_id
    → claim(id)

document_version_id
    → document_version(id)
```

---

Example Metadata

```json
{
  "camera": "iPhone 15",
  "gpsPresent": true,
  "captureDate": "2026-06-01"
}
```

---

Indexes

```sql
CREATE INDEX idx_analysis_result_document_version
ON processing.analysis_result(document_version_id);

CREATE INDEX idx_analysis_result_duplicate
ON processing.analysis_result(duplicate_detected);
```

---

# Incremental Reprocessing Flow

Claim Contains

```text
Doc1 V1
Doc2 V1
Doc3 V1
```

Customer Uploads

```text
Doc3 V2
```

System Creates

```text
OCR Job
for Doc3 V2

Analysis Job
for Doc3 V2
```

Only.

No processing for:

```text
Doc1 V1

Doc2 V1
```

---

# Claim Snapshot Builder

Before Fraud Analysis:

System Loads

```text
Current Active Versions
```

Example:

```text
Doc1 → V1

Doc2 → V1

Doc3 → V2
```

↓

Build Snapshot

↓

Fraud Engine

---

# Processing Domain ERD

Claim

↓

ClaimProcessingState

---

DocumentVersion

↓

OCRJob

↓

OCRResult

↓

OCRFieldExtraction

---

DocumentVersion

↓

AnalysisJob

↓

AnalysisResult

---

# Query Optimization Strategy

Common Queries

```sql
WHERE status = 'PENDING'

WHERE claim_id = ?

WHERE document_version_id = ?

WHERE locked_at < ?
```

Indexes support worker access patterns.

---

# Processing Domain Summary

Tables

```text
claim_processing_state

ocr_job

analysis_job

ocr_result

ocr_field_extraction

analysis_result
```

Features

```text
Worker Recovery              YES

Incremental Reprocessing     YES

OCR Metadata Storage         YES

OCR Payload In S3            YES

Image Analysis              YES

Claim Snapshot Builder       YES

Fraud Orchestration          YES
```

Core Entity

```text
claim_processing_state
```

Orchestrator Ready

```text
YES
```
