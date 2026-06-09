# 08.3 Document Events

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | Event Design          |
| Document Number | 08.3                  |
| Document Name   | Document Events       |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

This document defines all events produced by the Document Module.

The Document Module is one of the most important producers in ClaimLens because document events initiate:

* OCR Processing
* Image Analysis Processing
* Incremental Reprocessing
* Fraud Evaluation Workflows
* Analytics Updates
* Audit Logging

Document events drive the majority of asynchronous processing in the platform.

---

# 2. Document Event Catalog

| Event                         | Category             |
| ----------------------------- | -------------------- |
| DOCUMENT_UPLOADED             | Domain + Integration |
| DOCUMENT_VERSION_CREATED      | Domain + Integration |
| DOCUMENT_VALIDATED            | Domain               |
| DOCUMENT_PROCESSING_REQUESTED | Integration          |
| DOCUMENT_REPROCESS_REQUESTED  | Integration          |
| DOCUMENT_VERSION_ACTIVATED    | Domain               |
| DOCUMENT_VERSION_ARCHIVED     | Domain               |
| DOCUMENT_DELETED              | Domain               |
| DOCUMENT_RESTORED             | Domain               |

---

# 3. DOCUMENT_UPLOADED

## Category

```text id="3tq6m2"
Domain Event
Integration Event
```

---

## Trigger

A new document is uploaded for a claim.

---

## Producer

```text id="f1a2vd"
Document Module
```

---

## Consumers

```text id="6sy4zu"
Processing Module
Analytics Module
Audit Module
Notification Module
```

---

## Payload

```json id="q4d3jj"
{
  "claimId": 10001,
  "documentId": 2001,
  "versionId": 3001,
  "documentType": "VEHICLE_RC",
  "fileName": "vehicle_rc.pdf",
  "uploadedBy": 101
}
```

---

## Business Rules

* File must be uploaded to S3 successfully.
* Document metadata must be committed.
* Event published after transaction commit.

---

## Workflow

```text id="fwvhh8"
DOCUMENT_UPLOADED
       ↓
DOCUMENT_PROCESSING_REQUESTED
       ↓
OCR_JOB_CREATED
```

---

# 4. DOCUMENT_VERSION_CREATED

## Category

```text id="54tmof"
Domain Event
Integration Event
```

---

## Trigger

A new version of an existing document is uploaded.

Example:

```text id="j5gkz7"
RC V1
   ↓
RC V2
```

---

## Producer

```text id="mxh5hm"
Document Module
```

---

## Consumers

```text id="gkr3hz"
Processing Module
Fraud Module
Analytics Module
Audit Module
```

---

## Payload

```json id="xg1j5l"
{
  "claimId": 10001,
  "documentId": 2001,
  "oldVersionId": 3001,
  "newVersionId": 3002,
  "documentType": "VEHICLE_RC"
}
```

---

## Business Rules

* New version becomes active.
* Previous version remains historical.
* Version history preserved.

---

## Workflow

```text id="ktpnnr"
DOCUMENT_VERSION_CREATED
         ↓
DOCUMENT_VERSION_ACTIVATED
         ↓
DOCUMENT_REPROCESS_REQUESTED
```

---

# 5. DOCUMENT_VALIDATED

## Category

```text id="v49b40"
Domain Event
```

---

## Trigger

Document validation completed successfully.

Validation Examples:

```text id="3hplq7"
File Type Validation
Size Validation
Virus Scan
Format Validation
```

---

## Producer

```text id="v25u1s"
Document Module
```

---

## Consumers

```text id="vhx1yf"
Audit Module
Analytics Module
```

---

## Payload

```json id="o7z9nf"
{
  "claimId": 10001,
  "documentId": 2001,
  "versionId": 3001,
  "validationStatus": "PASSED"
}
```

---

# 6. DOCUMENT_PROCESSING_REQUESTED

## Category

```text id="62nklr"
Integration Event
```

---

## Trigger

Document is ready for OCR and Analysis processing.

---

## Producer

```text id="m5yz0u"
Document Module
```

---

## Consumers

```text id="l2zw7i"
Processing Module
Audit Module
```

---

## Payload

```json id="cl7j4m"
{
  "claimId": 10001,
  "documentId": 2001,
  "versionId": 3001,
  "documentType": "VEHICLE_RC"
}
```

---

## Business Rules

* Document validation must pass.
* Active document version required.

---

## Workflow

```text id="x4k73s"
DOCUMENT_PROCESSING_REQUESTED
          ↓
OCR_JOB_CREATED
          ↓
OCR_COMPLETED
          ↓
ANALYSIS_JOB_CREATED
```

---

# 7. DOCUMENT_REPROCESS_REQUESTED

## Category

```text id="k9v6vh"
Integration Event
```

---

## Trigger

A document version requires reprocessing.

---

## Producer

```text id="i9yx2g"
Document Module
```

---

## Consumers

```text id="0azwzj"
Processing Module
Audit Module
Analytics Module
```

---

## Payload

```json id="pwf70q"
{
  "claimId": 10001,
  "documentId": 2001,
  "versionId": 3002,
  "reason": "NEW_VERSION_UPLOADED"
}
```

---

## Business Rules

Only selected version is reprocessed.

---

## Example

Current Snapshot:

```text id="qfg2kk"
DL V1
RC V1
POLICY V1
```

Upload:

```text id="k7m6vm"
RC V2
```

Reprocess:

```text id="y2dxwu"
RC V2 ONLY
```

Do Not Reprocess:

```text id="1v51qr"
DL V1
POLICY V1
```

---

# 8. DOCUMENT_VERSION_ACTIVATED

## Category

```text id="l7z3yr"
Domain Event
```

---

## Trigger

A document version becomes active.

---

## Producer

```text id="4jtdf7"
Document Module
```

---

## Consumers

```text id="o7f2cs"
Fraud Module
Analytics Module
Audit Module
```

---

## Payload

```json id="iv9l9v"
{
  "documentId": 2001,
  "versionId": 3002,
  "documentType": "VEHICLE_RC"
}
```

---

## Business Rules

Only one active version allowed per document.

---

# 9. DOCUMENT_VERSION_ARCHIVED

## Category

```text id="3o6jso"
Domain Event
```

---

## Trigger

Existing version becomes inactive.

---

## Producer

```text id="cv3t9n"
Document Module
```

---

## Consumers

```text id="v1nqg2"
Audit Module
Analytics Module
```

---

## Payload

```json id="z9p42w"
{
  "documentId": 2001,
  "versionId": 3001
}
```

---

# 10. DOCUMENT_DELETED

## Category

```text id="f9r5mb"
Domain Event
```

---

## Trigger

Document soft-deleted.

---

## Producer

```text id="h7tkys"
Document Module
```

---

## Consumers

```text id="ph8zri"
Audit Module
Analytics Module
Notification Module
```

---

## Payload

```json id="khn6wd"
{
  "claimId": 10001,
  "documentId": 2001,
  "deletedBy": 101
}
```

---

## Business Rules

* Soft delete only.
* Historical versions preserved.

---

# 11. DOCUMENT_RESTORED

## Category

```text id="0m3qci"
Domain Event
```

---

## Trigger

Deleted document restored.

---

## Producer

```text id="0v0p3z"
Document Module
```

---

## Consumers

```text id="s34ndt"
Audit Module
Analytics Module
```

---

## Payload

```json id="w7a7u5"
{
  "claimId": 10001,
  "documentId": 2001,
  "restoredBy": 101
}
```

---

# 12. Document Processing Workflow

## Initial Upload Flow

```text id="s6jzg8"
DOCUMENT_UPLOADED
          ↓
DOCUMENT_VALIDATED
          ↓
DOCUMENT_PROCESSING_REQUESTED
          ↓
OCR_JOB_CREATED
          ↓
OCR_COMPLETED
          ↓
ANALYSIS_JOB_CREATED
          ↓
ANALYSIS_COMPLETED
```

---

## Version Upload Flow

```text id="paxqsm"
DOCUMENT_VERSION_CREATED
          ↓
DOCUMENT_VERSION_ACTIVATED
          ↓
DOCUMENT_REPROCESS_REQUESTED
          ↓
OCR_JOB_CREATED
          ↓
ANALYSIS_JOB_CREATED
```

---

# 13. Active Version Rules

Fraud Engine always evaluates:

```text id="0njlwm"
ACTIVE DOCUMENT VERSIONS ONLY
```

Example:

```text id="n25p0l"
DL V1
RC V2
POLICY V1
```

Fraud snapshot uses:

```text id="8guxv5"
DL V1
RC V2
POLICY V1
```

Historical versions ignored.

---

# 14. Correlation Strategy

All document events inherit:

```text id="85z25j"
claim workflow correlationId
```

Example:

```text id="h1e23f"
CLAIM_CREATED
DOCUMENT_UPLOADED
OCR_COMPLETED
ANALYSIS_COMPLETED
FRAUD_EVALUATED
```

Same correlation chain.

---

# 15. Idempotency Requirements

Consumers must safely process duplicate events:

```text id="y9zmp8"
DOCUMENT_UPLOADED
DOCUMENT_PROCESSING_REQUESTED
DOCUMENT_REPROCESS_REQUESTED
```

without creating duplicate jobs.

---

# 16. Monitoring Metrics

Prometheus Metrics:

```text id="53ymw7"
document_uploaded_total

document_version_created_total

document_processing_requested_total

document_reprocess_requested_total

document_event_failures_total
```

---

# 17. Security Requirements

Event payloads must never contain:

```text id="1u2v2u"
Document Content
OCR Content
Customer PII
S3 Credentials
```

Only identifiers and metadata allowed.

---

# 18. Future Events (V2)

Reserved for future enhancements:

```text id="mdrq5h"
DOCUMENT_SIGNATURE_VERIFIED

DOCUMENT_CLASSIFIED

DOCUMENT_DUPLICATE_DETECTED

DOCUMENT_TAMPERING_DETECTED
```

Not implemented in V1.

---

# Approval

This document defines the Document Domain Event Catalog for ClaimLens and serves as the implementation reference for document lifecycle management, version control, OCR initiation, analysis processing, incremental reprocessing, fraud evaluation inputs, analytics aggregation, and audit tracking.
