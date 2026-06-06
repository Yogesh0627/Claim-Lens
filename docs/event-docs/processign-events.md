# 08.6 Processing Events

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | RiskLens Technologies |
| Version         | V1                    |
| Document Type   | Event Design          |
| Document Number | 08.6                  |
| Document Name   | Processing Events     |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

This document defines all events produced by the Processing Module.

The Processing Module orchestrates:

* OCR Processing
* Image Analysis
* Reprocessing
* Worker Recovery
* Fraud Evaluation Coordination

The Processing Module acts as the workflow orchestrator between:

```text
Document Module
      ↓
OCR Service
      ↓
Analysis Service
      ↓
Fraud Module
```

---

# 2. Processing Event Catalog

| Event                           | Category    |
| ------------------------------- | ----------- |
| OCR_JOB_CREATED                 | Integration |
| OCR_STARTED                     | System      |
| OCR_COMPLETED                   | Integration |
| OCR_FAILED                      | System      |
| ANALYSIS_JOB_CREATED            | Integration |
| ANALYSIS_STARTED                | System      |
| ANALYSIS_COMPLETED              | Integration |
| ANALYSIS_FAILED                 | System      |
| DOCUMENT_REPROCESSING_STARTED   | Integration |
| DOCUMENT_REPROCESSING_COMPLETED | Integration |
| CLAIM_PROCESSING_COMPLETED      | Integration |
| FRAUD_EVALUATION_QUEUED         | Integration |
| PROCESSING_RECOVERED            | System      |

---

# 3. OCR_JOB_CREATED

## Category

```text
Integration Event
```

---

## Trigger

OCR job created for document version.

---

## Producer

```text
Processing Module
```

---

## Consumers

```text
OCR Service
Analytics Module
Audit Module
```

---

## Source Event

```text
DOCUMENT_UPLOADED
DOCUMENT_VERSION_CREATED
```

---

## Payload

```json
{
  "jobId": 1001,
  "claimId": 10001,
  "documentId": 2001,
  "versionId": 3001,
  "documentType": "VEHICLE_RC"
}
```

---

## Business Rules

* One OCR job per document version.
* Active version required.

---

# 4. OCR_STARTED

## Category

```text
System Event
```

---

## Trigger

OCR worker begins processing.

---

## Producer

```text
OCR Service
```

---

## Consumers

```text
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "jobId": 1001,
  "startedAt": "2026-06-01T10:00:00Z"
}
```

---

# 5. OCR_COMPLETED

## Category

```text
Integration Event
```

---

## Trigger

OCR processing completed successfully.

---

## Producer

```text
OCR Service
```

---

## Consumers

```text
Processing Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "jobId": 1001,
  "claimId": 10001,
  "documentId": 2001,
  "versionId": 3001,
  "ocrResultId": 5001,
  "confidenceScore": 96.4
}
```

---

## Workflow

```text
OCR_COMPLETED
      ↓
ANALYSIS_JOB_CREATED
```

---

# 6. OCR_FAILED

## Category

```text
System Event
```

---

## Trigger

OCR processing fails.

---

## Producer

```text
OCR Service
```

---

## Consumers

```text
Recovery Worker
Analytics Module
Audit Module
Notification Module
```

---

## Payload

```json
{
  "jobId": 1001,
  "failureReason": "OCR_TIMEOUT"
}
```

---

# 7. ANALYSIS_JOB_CREATED

## Category

```text
Integration Event
```

---

## Trigger

OCR completed successfully.

---

## Producer

```text
Processing Module
```

---

## Consumers

```text
Analysis Service
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "jobId": 2001,
  "claimId": 10001,
  "documentId": 2001,
  "versionId": 3001
}
```

---

# 8. ANALYSIS_STARTED

## Category

```text
System Event
```

---

## Trigger

Analysis worker begins processing.

---

## Producer

```text
Analysis Service
```

---

# 9. ANALYSIS_COMPLETED

## Category

```text
Integration Event
```

---

## Trigger

Analysis completed successfully.

---

## Producer

```text
Analysis Service
```

---

## Consumers

```text
Processing Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "jobId": 2001,
  "claimId": 10001,
  "documentId": 2001,
  "versionId": 3001,
  "analysisResultId": 7001
}
```

---

# 10. ANALYSIS_FAILED

## Category

```text
System Event
```

---

## Trigger

Analysis processing fails.

---

## Producer

```text
Analysis Service
```

---

## Consumers

```text
Recovery Worker
Analytics Module
Audit Module
Notification Module
```

---

## Payload

```json
{
  "jobId": 2001,
  "failureReason": "IMAGE_CORRUPTED"
}
```

---

# 11. DOCUMENT_REPROCESSING_STARTED

## Category

```text
Integration Event
```

---

## Trigger

New document version uploaded.

---

## Producer

```text
Processing Module
```

---

## Consumers

```text
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "claimId": 10001,
  "documentId": 2001,
  "versionId": 3002
}
```

---

# 12. DOCUMENT_REPROCESSING_COMPLETED

## Category

```text
Integration Event
```

---

## Trigger

Selected document version fully reprocessed.

---

## Producer

```text
Processing Module
```

---

## Consumers

```text
Fraud Module
Analytics Module
Audit Module
```

---

## Business Rules

Only affected version processed.

---

# 13. CLAIM_PROCESSING_COMPLETED

## Category

```text
Integration Event
```

---

## Trigger

All required processing completed.

---

## Producer

```text
Processing Orchestrator
```

---

## Consumers

```text
Assignment Module
Fraud Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "claimId": 10001,
  "processedDocuments": 5
}
```

---

## Completion Criteria

```text
All Required OCR Complete
All Required Analysis Complete
```

---

## Important

This event replaces:

```text
CLAIM_READY_FOR_ASSIGNMENT
```

from earlier design.

---

# 14. FRAUD_EVALUATION_QUEUED

## Category

```text
Integration Event
```

---

## Trigger

Processing Orchestrator validates completion.

---

## Producer

```text
Processing Orchestrator
```

---

## Consumers

```text
Fraud Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "claimId": 10001
}
```

---

# 15. Race Condition Prevention

Only:

```text
Processing Orchestrator
```

may publish:

```text
FRAUD_EVALUATION_QUEUED
```

Never:

```text
OCR Service
Analysis Service
```

This prevents duplicate fraud evaluations.

---

# 16. PROCESSING_RECOVERED

## Category

```text
System Event
```

---

## Trigger

Recovery worker resets abandoned jobs.

---

## Producer

```text
Recovery Worker
```

---

## Consumers

```text
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "jobId": 1001,
  "jobType": "OCR"
}
```

---

# 17. Processing Workflow

## Standard Workflow

```text
DOCUMENT_UPLOADED
         ↓
OCR_JOB_CREATED
         ↓
OCR_STARTED
         ↓
OCR_COMPLETED
         ↓
ANALYSIS_JOB_CREATED
         ↓
ANALYSIS_STARTED
         ↓
ANALYSIS_COMPLETED
         ↓
CLAIM_PROCESSING_COMPLETED
         ↓
FRAUD_EVALUATION_QUEUED
```

---

## Incremental Reprocessing Workflow

```text
DOCUMENT_VERSION_CREATED
          ↓
DOCUMENT_REPROCESSING_STARTED
          ↓
OCR_JOB_CREATED
          ↓
ANALYSIS_JOB_CREATED
          ↓
DOCUMENT_REPROCESSING_COMPLETED
          ↓
FRAUD_EVALUATION_QUEUED
```

---

# 18. Recovery Strategy

Workers use:

```sql
FOR UPDATE SKIP LOCKED
```

Recovery Worker resets:

```text
PROCESSING
```

jobs exceeding timeout thresholds.

---

# 19. Monitoring Metrics

```text
ocr_jobs_created_total

ocr_completed_total

ocr_failed_total

analysis_completed_total

analysis_failed_total

claim_processing_completed_total

processing_recovery_total
```

---

# 20. Security Requirements

Events must never contain:

```text
OCR Payload
Analysis Payload
Customer PII
Raw Documents
```

Only identifiers and metadata.

---

# Approval

This document defines the Processing Event Catalog for ClaimLens and serves as the implementation reference for OCR orchestration, analysis orchestration, recovery workflows, incremental reprocessing, race-condition prevention, fraud evaluation coordination, analytics aggregation, and audit tracking.
