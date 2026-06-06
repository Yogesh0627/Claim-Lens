# Database Design Part 7 - Document Domain

Status: Draft

Version: 1.0

Owner: RiskLens Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* document
* document_version

The Document Domain manages all claim-related files uploaded to the platform.

Examples:

* Accident Photos
* Police Reports
* Repair Estimates
* Vehicle RC
* Driving License
* Medical Reports (Future)

This domain serves as the foundation for:

* OCR Processing
* Image Analysis
* Fraud Detection
* Investigation Review
* Customer Re-Submission

---

# Design Principles

Documents are immutable.

A document itself never changes.

Instead:

```text
Document
    ↓
DocumentVersion
```

Every new upload creates a new version.

Benefits:

* Full Auditability
* Version History
* Easy Rollback
* Incremental Reprocessing

---

# Storage Strategy

Files are NOT stored in PostgreSQL.

Files are stored in:

```text
AWS S3
```

PostgreSQL stores only:

```text
Metadata

File References

Version Information

Processing Status
```

---

# Table: document

Purpose:

Represents a logical document attached to a claim.

Schema:

public

---

Columns

| Column             | Type         | Constraints   |
| ------------------ | ------------ | ------------- |
| id                 | BIGSERIAL    | PRIMARY KEY   |
| tenant_id          | BIGINT       | NOT NULL      |
| claim_id           | BIGINT       | NOT NULL      |
| document_type_id   | BIGINT       | NOT NULL      |
| document_name      | VARCHAR(255) | NOT NULL      |
| current_version_id | BIGINT       |               |
| status             | VARCHAR(50)  | NOT NULL      |
| created_at         | TIMESTAMP    | NOT NULL      |
| created_by         | BIGINT       |               |
| updated_at         | TIMESTAMP    |               |
| updated_by         | BIGINT       |               |
| is_deleted         | BOOLEAN      | DEFAULT FALSE |
| deleted_at         | TIMESTAMP    |               |
| deleted_by         | BIGINT       |               |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)

claim_id
    → claim(id)

document_type_id
    → document_type(id)
```

---

Unique Constraints

None.

Multiple documents of same type may exist.

Example:

```text
Accident Photo 1

Accident Photo 2

Accident Photo 3
```

---

Indexes

```sql
CREATE INDEX idx_document_tenant
ON document(tenant_id);

CREATE INDEX idx_document_claim
ON document(claim_id);

CREATE INDEX idx_document_type
ON document(document_type_id);

CREATE INDEX idx_document_status
ON document(status);
```

---

Document Status Values

```text
ACTIVE

SUPERSEDED

ARCHIVED
```

---

# Table: document_version

Purpose:

Represents a physical uploaded file.

Every upload creates a new version.

Schema:

public

---

Columns

| Column             | Type         | Constraints |
| ------------------ | ------------ | ----------- |
| id                 | BIGSERIAL    | PRIMARY KEY |
| tenant_id          | BIGINT       | NOT NULL    |
| document_id        | BIGINT       | NOT NULL    |
| version_number     | INTEGER      | NOT NULL    |
| file_name          | VARCHAR(500) | NOT NULL    |
| original_file_name | VARCHAR(500) | NOT NULL    |
| mime_type          | VARCHAR(100) | NOT NULL    |
| file_size_bytes    | BIGINT       | NOT NULL    |
| file_checksum      | VARCHAR(255) |             |
| s3_bucket          | VARCHAR(255) | NOT NULL    |
| s3_object_key      | TEXT         | NOT NULL    |
| upload_source      | VARCHAR(50)  | NOT NULL    |
| processing_status  | VARCHAR(50)  | NOT NULL    |
| uploaded_at        | TIMESTAMP    | NOT NULL    |
| uploaded_by        | BIGINT       | NOT NULL    |
| created_at         | TIMESTAMP    | NOT NULL    |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)

document_id
    → document(id)

uploaded_by
    → user(id)
```

---

Version Constraints

```sql
CREATE UNIQUE INDEX uq_document_version
ON document_version(
    document_id,
    version_number
);
```

---

Indexes

```sql
CREATE INDEX idx_document_version_document
ON document_version(document_id);

CREATE INDEX idx_document_version_processing
ON document_version(processing_status);

CREATE INDEX idx_document_version_uploaded_at
ON document_version(uploaded_at);
```

---

Processing Status Values

```text
PENDING

OCR_PENDING

OCR_COMPLETE

ANALYSIS_PENDING

ANALYSIS_COMPLETE

FAILED

READY
```

---

Upload Sources

```text
CUSTOMER

EMPLOYEE

INVESTIGATOR

SYSTEM
```

---

# S3 Storage Structure

Recommended Structure

```text
tenant-id/
│
├── claims/
│   ├── claim-id/
│   │   ├── documents/
│   │   │   ├── document-id/
│   │   │   │   ├── v1.pdf
│   │   │   │   ├── v2.pdf
│   │   │   │   └── v3.pdf
```

---

Example

```text
tenant-1001/
claims/5001/
documents/9001/
v1.pdf
```

---

# Versioning Strategy

Example

Document:

```text
Police Report
```

Versions:

```text
V1 Uploaded

V2 Uploaded

V3 Uploaded
```

Current Document

```text
current_version_id = V3
```

Historical Versions

```text
V1
V2
```

remain immutable.

---

# Reprocessing Strategy

Claim Contains:

```text
Photo V1

Police Report V1

Repair Estimate V1
```

Customer uploads:

```text
Repair Estimate V2
```

System Processes:

```text
Repair Estimate V2 Only
```

NOT:

```text
Photo V1

Police Report V1
```

---

# Active Version Resolution

When building a claim snapshot:

System loads:

```sql
document.current_version_id
```

Example:

```text
Photo → V1

Police Report → V1

Repair Estimate → V2
```

This creates:

```text
Current Claim Snapshot
```

for Fraud Analysis.

---

# File Validation Rules

Before Upload Acceptance:

Validate:

```text
File Extension

Mime Type

File Size

Corruption Check
```

---

Allowed Types (V1)

```text
PDF

PNG

JPG

JPEG
```

---

Max File Size

```text
25 MB
```

(Configurable)

---

# Duplicate File Detection

Use:

```text
file_checksum
```

Generated:

```text
SHA-256
```

Purpose:

Detect identical uploads.

---

Example

```text
File A Hash
=
File B Hash
```

↓

Potential Duplicate

---

# Document Lifecycle

Created

↓

Version Uploaded

↓

OCR Processing

↓

Image Analysis

↓

Fraud Evaluation

↓

Investigation Review

↓

Approved / Replaced

↓

Archived

---

# Relationship Model

Claim

↓

Document

↓

DocumentVersion

---

Document

↓

DocumentType

---

DocumentVersion

↓

OCRResult

(Future Relationship)

---

DocumentVersion

↓

AnalysisResult

(Future Relationship)

---

# Query Optimization Strategy

Most Common Queries

```sql
WHERE claim_id = ?

WHERE document_id = ?

WHERE processing_status = ?

WHERE uploaded_at >= ?
```

Indexes added accordingly.

---

# Document Domain ERD

Claim

```text
id (PK)
```

↓

Document

```text
id (PK)

claim_id (FK)

current_version_id
```

↓

DocumentVersion

```text
id (PK)

document_id (FK)

version_number
```

---

# Future Enhancements

V2

```text
Document Classification

Automatic Document Type Detection

Virus Scanning

Document Expiry Rules
```

---

# Document Domain Summary

Tables

```text
document

document_version
```

Storage

```text
AWS S3
```

Versioning

```text
Supported
```

Incremental Reprocessing

```text
Supported
```

OCR Integration

```text
Supported
```

Fraud Integration

```text
Supported
```

Auditability

```text
Full Version History
```

Core Principle

```text
Documents Are Immutable

Versions Are Append-Only
```




# Database Design Part 8 - Assignment Domain

Status: Draft

Version: 1.0

Owner: RiskLens Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* assignment
* assignment_history
* assignment_queue

The Assignment Domain manages:

* Investigator Assignment
* Auto Assignment
* Manual Assignment
* Reassignment
* Workload Distribution
* Assignment Audit Trail

This domain is responsible for moving a claim from:

```text
AWAITING_ASSIGNMENT
```

to

```text
UNDER_INVESTIGATION
```

---

# Assignment Design Principles

A claim can have:

```text
Many Assignments
```

over its lifetime.

Examples:

```text
Assignment 1
    ↓
Rejected

Assignment 2
    ↓
Accepted

Assignment 3
    ↓
Escalated
```

Therefore:

```text
Claim
    ↓
Assignment
```

is:

```text
One-To-Many
```

---

# Assignment Lifecycle

Claim

↓

Fraud Analysis Complete

↓

Assignment Queue

↓

Investigator Selected

↓

Assignment Created

↓

Accepted

OR

Rejected

↓

Investigation Created

---

# Table: assignment

Purpose:

Represents a claim assignment to an investigator.

Schema:

public

---

Columns

| Column               | Type        | Constraints |
| -------------------- | ----------- | ----------- |
| id                   | BIGSERIAL   | PRIMARY KEY |
| tenant_id            | BIGINT      | NOT NULL    |
| claim_id             | BIGINT      | NOT NULL    |
| investigator_id      | BIGINT      | NOT NULL    |
| assigned_by          | BIGINT      |             |
| assignment_policy_id | BIGINT      |             |
| assignment_method    | VARCHAR(50) | NOT NULL    |
| priority             | VARCHAR(50) | NOT NULL    |
| status               | VARCHAR(50) | NOT NULL    |
| assigned_at          | TIMESTAMP   | NOT NULL    |
| accepted_at          | TIMESTAMP   |             |
| rejected_at          | TIMESTAMP   |             |
| completed_at         | TIMESTAMP   |             |
| rejection_reason     | TEXT        |             |
| due_at               | TIMESTAMP   |             |
| created_at           | TIMESTAMP   | NOT NULL    |
| created_by           | BIGINT      |             |
| updated_at           | TIMESTAMP   |             |
| updated_by           | BIGINT      |             |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)

claim_id
    → claim(id)

investigator_id
    → user(id)

assigned_by
    → user(id)

assignment_policy_id
    → assignment_policy(id)
```

---

Assignment Methods

```text
AUTO

MANUAL

REASSIGNMENT
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

Status Values

```text
PENDING_ACCEPTANCE

ACCEPTED

REJECTED

COMPLETED

CANCELLED
```

---

Indexes

```sql
CREATE INDEX idx_assignment_tenant
ON assignment(tenant_id);

CREATE INDEX idx_assignment_claim
ON assignment(claim_id);

CREATE INDEX idx_assignment_investigator
ON assignment(investigator_id);

CREATE INDEX idx_assignment_status
ON assignment(status);

CREATE INDEX idx_assignment_priority
ON assignment(priority);

CREATE INDEX idx_assignment_due_at
ON assignment(due_at);

CREATE INDEX idx_assignment_investigator_status
ON assignment(investigator_id, status);
```

---

Business Rule

Only one active assignment allowed per claim.

```sql
CREATE UNIQUE INDEX uq_claim_active_assignment
ON assignment(claim_id)
WHERE status IN (
    'PENDING_ACCEPTANCE',
    'ACCEPTED'
);
```

---

# Table: assignment_history

Purpose:

Stores immutable assignment events.

Provides full auditability.

Schema:

public

---

Columns

| Column                   | Type        | Constraints |
| ------------------------ | ----------- | ----------- |
| id                       | BIGSERIAL   | PRIMARY KEY |
| tenant_id                | BIGINT      | NOT NULL    |
| assignment_id            | BIGINT      | NOT NULL    |
| claim_id                 | BIGINT      | NOT NULL    |
| event_type               | VARCHAR(50) | NOT NULL    |
| previous_investigator_id | BIGINT      |             |
| new_investigator_id      | BIGINT      |             |
| reason                   | TEXT        |             |
| performed_by             | BIGINT      |             |
| created_at               | TIMESTAMP   | NOT NULL    |

---

Foreign Keys

```sql
assignment_id
    → assignment(id)

claim_id
    → claim(id)

performed_by
    → user(id)
```

---

Event Types

```text
CREATED

ACCEPTED

REJECTED

REASSIGNED

ESCALATED

COMPLETED

CANCELLED
```

---

Indexes

```sql
CREATE INDEX idx_assignment_history_assignment
ON assignment_history(assignment_id);

CREATE INDEX idx_assignment_history_claim
ON assignment_history(claim_id);

CREATE INDEX idx_assignment_history_created_at
ON assignment_history(created_at);
```

---

Example Timeline

```text
CREATED
    ↓
REJECTED
    ↓
REASSIGNED
    ↓
ACCEPTED
    ↓
COMPLETED
```

---

# Table: assignment_queue

Purpose:

Temporary holding area before assignment.

Used by:

* Auto Assignment Engine
* Workload Balancing
* Retry Logic

Schema:

public

---

Columns

| Column               | Type         | Constraints |
| -------------------- | ------------ | ----------- |
| id                   | BIGSERIAL    | PRIMARY KEY |
| tenant_id            | BIGINT       | NOT NULL    |
| claim_id             | BIGINT       | NOT NULL    |
| assignment_policy_id | BIGINT       |             |
| priority             | VARCHAR(50)  | NOT NULL    |
| queue_reason         | VARCHAR(100) | NOT NULL    |
| status               | VARCHAR(50)  | NOT NULL    |
| retry_count          | INTEGER      | DEFAULT 0   |
| queued_at            | TIMESTAMP    | NOT NULL    |
| processed_at         | TIMESTAMP    |             |
| created_at           | TIMESTAMP    | NOT NULL    |

---

Foreign Keys

```sql
claim_id
    → claim(id)

assignment_policy_id
    → assignment_policy(id)
```

---

Queue Reasons

```text
INITIAL_ASSIGNMENT

REASSIGNMENT

ESCALATION

MANUAL_REVIEW
```

---

Status Values

```text
PENDING

PROCESSING

COMPLETED

FAILED
```

---

Indexes

```sql
CREATE INDEX idx_assignment_queue_status
ON assignment_queue(status);

CREATE INDEX idx_assignment_queue_priority
ON assignment_queue(priority);

CREATE INDEX idx_assignment_queue_claim
ON assignment_queue(claim_id);

CREATE INDEX idx_assignment_queue_tenant
ON assignment_queue(tenant_id);
```

---

# Auto Assignment Strategy

Assignment Queue

↓

Load Assignment Policy

↓

Find Eligible Investigators

Filters

```text
ACTIVE

Same Tenant

Correct Region

Correct Branch

Not On Leave

Workload Within Limits
```

↓

Apply Strategy

```text
ROUND_ROBIN

LEAST_LOADED
```

↓

Create Assignment

↓

Remove Queue Entry

---

# Investigator Workload Query

Common Query

```sql
SELECT investigator_id,
COUNT(*)
FROM assignment
WHERE status = 'ACCEPTED'
GROUP BY investigator_id;
```

Used by:

```text
LEAST_LOADED
```

strategy.

---

# Reassignment Flow

Assignment

↓

Rejected

↓

Assignment History Event

↓

Queue Entry Created

↓

New Investigator Selected

↓

New Assignment Created

---

# Escalation Flow

Assignment

↓

SLA Breach

↓

Queue Entry Created

↓

Manager Review

↓

Reassignment

OR

Escalation

---

# Assignment Domain ERD

Claim

↓

Assignment

↓

AssignmentHistory

---

Claim

↓

AssignmentQueue

---

User (Investigator)

↓

Assignment

---

# Query Optimization Strategy

Most Common Queries

```sql
WHERE investigator_id = ?

WHERE status = ?

WHERE tenant_id = ?

WHERE claim_id = ?

WHERE due_at <= NOW()
```

Indexes support all major access patterns.

---

# Assignment Domain Summary

Tables

```text
assignment

assignment_history

assignment_queue
```

Supports

```text
Auto Assignment            YES

Manual Assignment          YES

Round Robin                YES

Least Loaded               YES

Reassignment               YES

Escalation                 YES

Audit Trail                YES

Workload Tracking          YES
```

Core Entity

```text
assignment
```

Assignment Engine Ready

```text
YES
```
