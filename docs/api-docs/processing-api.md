# 07.10 Processing & OCR API

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | RiskLens Technologies |
| Version         | V1                    |
| Document Type   | API Design            |
| Document Number | 07.10                 |
| Document Name   | Processing & OCR API  |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

The Processing Module orchestrates all automated document processing activities within ClaimLens.

Responsibilities:

* OCR Job Management
* Analysis Job Management
* Processing State Tracking
* OCR Result Storage
* OCR Field Extraction
* Image Analysis Results
* Job Monitoring
* Job Recovery
* Incremental Reprocessing
* Fraud Engine Trigger Coordination

The Processing Module acts as the bridge between:

```text
Document Module
       ↓
OCR Service
       ↓
Analysis Service
       ↓
Fraud Engine
```

---

# 2. Domain Entities

Managed Entities:

```text
claim_processing_state
ocr_job
analysis_job
ocr_result
ocr_field_extraction
analysis_result
```

Related Entities:

```text
claim
document
document_version
fraud_score
fraud_alert
```

---

# 3. Processing Architecture

## Processing Flow

```text
Document Uploaded
       ↓
OCR Job Created
       ↓
OCR Service
       ↓
OCR Result Saved
       ↓
Analysis Job Created
       ↓
Analysis Service
       ↓
Analysis Result Saved
       ↓
Orchestrator Validation
       ↓
Fraud Engine Triggered
```

---

## Important Architecture Rule

Only:

```text
PROCESSING ORCHESTRATOR
```

may trigger Fraud Analysis.

Never:

```text
OCR SERVICE
ANALYSIS SERVICE
```

This prevents duplicate fraud execution and race conditions.

---

# 4. Job Lifecycle

Supported Statuses:

```text
PENDING
PROCESSING
COMPLETE
FAILED
```

Lifecycle:

```text
PENDING
   ↓
PROCESSING
   ↓
COMPLETE

OR

PROCESSING
   ↓
FAILED
```

---

# 5. Permission Matrix

| Permission           | Description                          |
| -------------------- | ------------------------------------ |
| PROCESSING_VIEW      | View processing status               |
| PROCESSING_REPROCESS | Trigger reprocessing                 |
| OCR_RESULT_VIEW      | View OCR results                     |
| ANALYSIS_RESULT_VIEW | View analysis results                |
| PROCESSING_ADMIN     | Administrative processing operations |

---

# 6. Claim Processing State APIs

---

## Get Claim Processing State

### Endpoint

```http
GET /api/v1/processing/claims/{claimId}/state
```

### Permissions

```text
PROCESSING_VIEW
```

### Success Response

```json
{
  "claimId": 10001,
  "ocrStatus": "COMPLETE",
  "analysisStatus": "COMPLETE",
  "fraudStatus": "COMPLETE",
  "lastUpdatedAt": "2026-06-01T12:00:00Z"
}
```

---

## Get Processing Summary

### Endpoint

```http
GET /api/v1/processing/claims/{claimId}/summary
```

### Success Response

```json
{
  "claimId": 10001,
  "documentsProcessed": 8,
  "ocrJobsCompleted": 8,
  "analysisJobsCompleted": 8,
  "fraudExecutionCompleted": true
}
```

---

# 7. OCR Job APIs

---

## Get OCR Job

### Endpoint

```http
GET /api/v1/processing/ocr-jobs/{jobId}
```

### Success Response

```json
{
  "jobId": 101,
  "documentId": 2001,
  "versionId": 3001,
  "status": "COMPLETE",
  "startedAt": "2026-06-01T10:00:00Z",
  "completedAt": "2026-06-01T10:00:08Z"
}
```

---

## List OCR Jobs

### Endpoint

```http
GET /api/v1/processing/ocr-jobs
```

### Query Parameters

```text
status
claimId
documentId
createdFrom
createdTo
page
size
```

---

## Retry OCR Job

### Endpoint

```http
POST /api/v1/processing/ocr-jobs/{jobId}/retry
```

### Permissions

```text
PROCESSING_REPROCESS
```

### Success Response

```json
{
  "message": "OCR retry initiated"
}
```

### Audit Event

```text
OCR_JOB_RETRIED
```

---

# 8. OCR Result APIs

---

## Get OCR Result

### Endpoint

```http
GET /api/v1/processing/ocr-results/{ocrResultId}
```

### Permissions

```text
OCR_RESULT_VIEW
```

### Success Response

```json
{
  "ocrResultId": 501,
  "documentId": 2001,
  "versionId": 3001,
  "confidenceScore": 96.4,
  "processingTimeMs": 3210,
  "payloadLocation": "s3://claimlens-ocr-results/..."
}
```

---

## Get OCR Result By Document Version

### Endpoint

```http
GET /api/v1/processing/documents/{documentId}/versions/{versionId}/ocr-result
```

### Success Response

```json
{
  "ocrResultId": 501,
  "confidenceScore": 96.4,
  "processingTimeMs": 3210
}
```

---

## Get OCR Payload URL

### Endpoint

```http
GET /api/v1/processing/ocr-results/{ocrResultId}/payload-url
```

### Description

Generate temporary S3 URL to access raw OCR payload.

### Success Response

```json
{
  "downloadUrl": "https://presigned-url",
  "expiresIn": 300
}
```

---

# 9. OCR Field Extraction APIs

---

## Get Extracted Fields

### Endpoint

```http
GET /api/v1/processing/ocr-results/{ocrResultId}/fields
```

### Success Response

```json
[
  {
    "fieldName": "vehicle_registration_number",
    "fieldValue": "DL01AB1234",
    "confidenceScore": 97.8
  },
  {
    "fieldName": "owner_name",
    "fieldValue": "Rahul Sharma",
    "confidenceScore": 94.2
  }
]
```

---

## Search OCR Fields

### Endpoint

```http
GET /api/v1/processing/ocr-fields/search
```

### Query Parameters

```text
claimId
fieldName
fieldValue
```

---

# 10. Analysis Job APIs

---

## Get Analysis Job

### Endpoint

```http
GET /api/v1/processing/analysis-jobs/{jobId}
```

### Success Response

```json
{
  "jobId": 701,
  "documentId": 2001,
  "versionId": 3001,
  "status": "COMPLETE",
  "startedAt": "2026-06-01T10:05:00Z",
  "completedAt": "2026-06-01T10:05:04Z"
}
```

---

## List Analysis Jobs

### Endpoint

```http
GET /api/v1/processing/analysis-jobs
```

### Query Parameters

```text
status
claimId
documentId
page
size
```

---

## Retry Analysis Job

### Endpoint

```http
POST /api/v1/processing/analysis-jobs/{jobId}/retry
```

### Success Response

```json
{
  "message": "Analysis retry initiated"
}
```

### Audit Event

```text
ANALYSIS_JOB_RETRIED
```

---

# 11. Analysis Result APIs

---

## Get Analysis Result

### Endpoint

```http
GET /api/v1/processing/analysis-results/{analysisResultId}
```

### Permissions

```text
ANALYSIS_RESULT_VIEW
```

### Success Response

```json
{
  "analysisResultId": 801,
  "documentId": 2001,
  "versionId": 3001,
  "analysisType": "IMAGE_TAMPERING",
  "riskScore": 12,
  "processingTimeMs": 1800
}
```

---

## Get Analysis Results For Document

### Endpoint

```http
GET /api/v1/processing/documents/{documentId}/analysis-results
```

### Success Response

```json
[
  {
    "analysisType": "IMAGE_TAMPERING",
    "riskScore": 12
  },
  {
    "analysisType": "METADATA_ANALYSIS",
    "riskScore": 5
  }
]
```

---

# 12. Reprocessing APIs

---

## Reprocess Document Version

### Endpoint

```http
POST /api/v1/processing/documents/{documentId}/versions/{versionId}/reprocess
```

### Permissions

```text
PROCESSING_REPROCESS
```

### Business Rules

Only selected version is reprocessed.

Example:

```text
DL V1
RC V2 ← Reprocess
Policy V1
```

Only:

```text
RC V2
```

is reprocessed.

### Success Response

```json
{
  "message": "Reprocessing started"
}
```

### Audit Event

```text
DOCUMENT_REPROCESS_STARTED
```

---

## Reprocess Entire Claim

### Endpoint

```http
POST /api/v1/processing/claims/{claimId}/reprocess
```

### Description

Reprocess active document versions only.

### Success Response

```json
{
  "message": "Claim reprocessing started"
}
```

### Audit Event

```text
CLAIM_REPROCESS_STARTED
```

---

# 13. Job Monitoring APIs

---

## Get Processing Dashboard

### Endpoint

```http
GET /api/v1/processing/dashboard
```

### Success Response

```json
{
  "pendingJobs": 12,
  "processingJobs": 4,
  "completedJobs": 2450,
  "failedJobs": 15
}
```

---

## Get Failed Jobs

### Endpoint

```http
GET /api/v1/processing/failed-jobs
```

### Query Parameters

```text
jobType
createdFrom
createdTo
```

### Success Response

```json
[
  {
    "jobId": 101,
    "jobType": "OCR",
    "status": "FAILED"
  }
]
```

---

# 14. Recovery Worker APIs

---

## Get Recovery Status

### Endpoint

```http
GET /api/v1/processing/recovery/status
```

### Success Response

```json
{
  "abandonedJobsFound": 5,
  "jobsRecovered": 5,
  "lastRunAt": "2026-06-01T11:00:00Z"
}
```

---

## Trigger Recovery

### Endpoint

```http
POST /api/v1/processing/recovery/run
```

### Permissions

```text
PROCESSING_ADMIN
```

### Success Response

```json
{
  "message": "Recovery worker executed"
}
```

### Audit Event

```text
RECOVERY_WORKER_EXECUTED
```

---

# 15. Orchestrator APIs

---

## Get Orchestrator Status

### Endpoint

```http
GET /api/v1/processing/orchestrator/status
```

### Success Response

```json
{
  "running": true,
  "lastExecutionAt": "2026-06-01T12:00:00Z"
}
```

---

## Trigger Fraud Evaluation

### Endpoint

```http
POST /api/v1/processing/claims/{claimId}/evaluate-fraud
```

### Permissions

```text
PROCESSING_ADMIN
```

### Description

Manual orchestrator-triggered fraud evaluation.

### Success Response

```json
{
  "message": "Fraud evaluation queued"
}
```

### Audit Event

```text
FRAUD_EVALUATION_TRIGGERED
```

---

# 16. Worker Processing Rules

## Concurrency Control

Workers must acquire jobs using:

```sql
FOR UPDATE SKIP LOCKED
```

Prevents:

* Duplicate processing
* Race conditions
* Concurrent job execution

---

## Recovery Rules

Abandoned jobs:

```text
PROCESSING
```

older than configured threshold are reset to:

```text
PENDING
```

by Recovery Worker.

---

## Fraud Execution Rules

Fraud evaluation starts only when:

```text
ALL REQUIRED OCR COMPLETE
ALL REQUIRED ANALYSIS COMPLETE
```

Validation is performed by:

```text
PROCESSING ORCHESTRATOR
```

---

# 17. Business Validation Rules

* OCR result must belong to document version.
* Analysis result must belong to document version.
* Reprocessing affects only selected versions.
* Historical versions remain immutable.
* Fraud engine cannot be triggered directly by OCR service.
* Fraud engine cannot be triggered directly by Analysis service.

---

# 18. Error Codes

| Error Code                 | Description                     |
| -------------------------- | ------------------------------- |
| OCR_JOB_NOT_FOUND          | OCR job missing                 |
| ANALYSIS_JOB_NOT_FOUND     | Analysis job missing            |
| OCR_RESULT_NOT_FOUND       | OCR result missing              |
| ANALYSIS_RESULT_NOT_FOUND  | Analysis result missing         |
| PROCESSING_STATE_NOT_FOUND | Processing state missing        |
| REPROCESS_ALREADY_RUNNING  | Reprocessing active             |
| JOB_ALREADY_COMPLETE       | Job already complete            |
| INVALID_DOCUMENT_VERSION   | Version invalid                 |
| RECOVERY_NOT_ALLOWED       | Recovery blocked                |
| FRAUD_TRIGGER_BLOCKED      | Fraud trigger validation failed |

---

# 19. Audit Events

```text
OCR_JOB_RETRIED
ANALYSIS_JOB_RETRIED

DOCUMENT_REPROCESS_STARTED
CLAIM_REPROCESS_STARTED

RECOVERY_WORKER_EXECUTED

FRAUD_EVALUATION_TRIGGERED
```

---

# 20. Integration Events

Produced Events:

```text
OCR_COMPLETED
OCR_FAILED

ANALYSIS_COMPLETED
ANALYSIS_FAILED

DOCUMENT_REPROCESSED

CLAIM_PROCESSING_COMPLETED

FRAUD_EVALUATION_QUEUED
```

Consumed By:

```text
DOCUMENT MODULE
FRAUD MODULE
NOTIFICATION MODULE
ANALYTICS MODULE
```

---

# 21. Performance Requirements

| API                    | Target   |
| ---------------------- | -------- |
| OCR Result Lookup      | < 200 ms |
| Analysis Result Lookup | < 200 ms |
| Processing Summary     | < 300 ms |
| Reprocess Trigger      | < 300 ms |
| Dashboard API          | < 500 ms |
| Failed Job Search      | < 500 ms |

---

# 22. Security Requirements

All APIs require:

```text
JWT Authentication
Tenant Validation
Permission Validation
Audit Logging
```

Additional Controls:

* OCR payload access through pre-signed URLs only.
* Raw OCR payloads never exposed directly.
* Cross-tenant processing access prohibited.
* Administrative processing operations restricted.

---

# Approval

This document defines the Processing & OCR API contract for ClaimLens and serves as the implementation reference for OCR workflows, analysis workflows, job orchestration, worker recovery, processing state management, incremental reprocessing, and fraud evaluation coordination.
