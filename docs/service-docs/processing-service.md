# 09.6 Processing Service Design

## Document Information

| Field           | Value                     |
| --------------- | ------------------------- |
| Project         | ClaimLens                 |
| Company         | Niyo Technologies     |
| Version         | V1                        |
| Document Type   | Service Design            |
| Document Number | 09.6                      |
| Document Name   | Processing Service Design |
| Status          | Approved                  |
| Last Updated    | June 2026                 |

---

# 1. Overview

The Processing Module orchestrates the complete document processing workflow.

Responsibilities:

* OCR Job Creation
* OCR Job Tracking
* Analysis Job Creation
* Analysis Job Tracking
* Processing State Management
* Incremental Reprocessing
* Recovery Handling
* Fraud Evaluation Coordination

The Processing Module acts as the workflow orchestrator between:

```text id="hgj6af"
Document Module
        ↓
OCR Service
        ↓
Analysis Service
        ↓
Fraud Module
```

---

# 2. Module Dependencies

## Consumed Events

```text id="4k4nvy"
DOCUMENT_UPLOADED

DOCUMENT_VERSION_CREATED

OCR_COMPLETED

OCR_FAILED

ANALYSIS_COMPLETED

ANALYSIS_FAILED
```

---

## Published Events

```text id="t9d74r"
OCR_JOB_CREATED

OCR_STARTED

OCR_COMPLETED

OCR_FAILED

ANALYSIS_JOB_CREATED

ANALYSIS_STARTED

ANALYSIS_COMPLETED

ANALYSIS_FAILED

DOCUMENT_REPROCESSING_STARTED

DOCUMENT_REPROCESSING_COMPLETED

CLAIM_PROCESSING_COMPLETED

FRAUD_EVALUATION_QUEUED

PROCESSING_RECOVERED
```

---

# 3. Package Structure

```text id="w5k9zc"
processing

├── controller
│   └── ProcessingController

├── service
│   ├── ProcessingOrchestrator
│   ├── OcrJobService
│   ├── AnalysisJobService
│   ├── ProcessingStateService
│   ├── RecoveryService
│   └── ProcessingServiceImpl

├── repository
│   ├── OcrJobRepository
│   ├── AnalysisJobRepository
│   ├── ProcessingStateRepository
│   └── OcrResultRepository

├── entity
│   ├── OcrJob
│   ├── AnalysisJob
│   ├── ProcessingState
│   ├── OcrResult
│   ├── OcrFieldExtraction
│   └── AnalysisResult

├── dto

├── mapper

├── validator

├── event

├── worker

├── exception
```

---

# 4. Domain Entities

Primary Entities:

```text id="nkt4vp"
OcrJob

AnalysisJob

ProcessingState
```

Result Entities:

```text id="6q8s2z"
OcrResult

OcrFieldExtraction

AnalysisResult
```

---

# 5. Processing Orchestrator

## Responsibilities

```text id="pkf5yl"
Workflow Coordination

State Tracking

Completion Detection

Fraud Trigger Coordination
```

---

## Critical Ownership

Only:

```text id="klr7bn"
ProcessingOrchestrator
```

may publish:

```text id="3q3mym"
FRAUD_EVALUATION_QUEUED
```

---

## Forbidden

```text id="ny3e9w"
OCR Service

Analysis Service
```

must never publish fraud events.

---

# 6. OCR Job Service

Responsibilities:

```text id="57l77v"
Create OCR Jobs

Track Status

Store OCR Results

Retry Failed Jobs
```

---

## Methods

```java id="2zdfvz"
createJob()

startJob()

completeJob()

failJob()

retryJob()
```

---

# 7. Analysis Job Service

Responsibilities:

```text id="03z5wo"
Create Analysis Jobs

Track Status

Store Results

Retry Failed Jobs
```

---

## Methods

```java id="v1htk4"
createJob()

startJob()

completeJob()

failJob()

retryJob()
```

---

# 8. Processing State Service

Purpose:

Track claim-level processing progress.

---

## Processing State Example

```text id="9w7c1i"
Claim 10001

Required Documents = 5

OCR Completed = 5

Analysis Completed = 5

Status = COMPLETE
```

---

## Methods

```java id="c7l2gr"
updateState()

markCompleted()

isProcessingComplete()
```

---

# 9. Controller Layer

## ProcessingController

Base Path

```http id="v9k1xa"
/api/v1/processing
```

---

## Endpoints

```http id="6qq3cw"
GET /processing/claims/{claimId}

GET /processing/ocr-jobs/{jobId}

GET /processing/analysis-jobs/{jobId}

POST /processing/reprocess/{documentId}
```

---

# 10. OCR Integration

## OCR Service Communication

V1 Strategy:

```text id="zrz1hl"
REST API
```

---

## Workflow

```text id="r6a0ob"
OCR_JOB_CREATED
        ↓
OCR Worker Picks Job
        ↓
Python OCR Service
        ↓
OCR_COMPLETED
```

---

## OCR Result Storage

Database Stores:

```text id="04xjlwm"
confidence_score

payload_location

processing_time_ms
```

---

Raw Payload:

```text id="u09tuh"
Stored In S3
```

---

# 11. Analysis Integration

## Analysis Service Communication

V1 Strategy:

```text id="kn53ru"
REST API
```

---

## Workflow

```text id="f1a3vk"
OCR_COMPLETED
       ↓
ANALYSIS_JOB_CREATED
       ↓
Python Analysis Service
       ↓
ANALYSIS_COMPLETED
```

---

# 12. Incremental Reprocessing

Example:

Current Snapshot:

```text id="04ccqf"
DL V1

RC V1

POLICY V1
```

Upload:

```text id="4vjlwm"
RC V2
```

---

Reprocess:

```text id="y8q4yq"
RC V2 ONLY
```

---

Do Not Reprocess:

```text id="l4zj5w"
DL V1

POLICY V1
```

---

# 13. Completion Detection

## Rule

Processing Complete When:

```text id="1d9ldu"
All Required OCR Complete

AND

All Required Analysis Complete
```

---

## Workflow

```text id="n8sv3r"
ANALYSIS_COMPLETED
         ↓
Processing State Updated
         ↓
isProcessingComplete()
         ↓
CLAIM_PROCESSING_COMPLETED
```

---

# 14. Fraud Trigger Strategy

## Critical Rule

Only:

```text id="bgjzmt"
ProcessingOrchestrator
```

may execute:

```text id="1wnhqt"
queueFraudEvaluation()
```

---

## Workflow

```text id="l8yw97"
CLAIM_PROCESSING_COMPLETED
         ↓
FRAUD_EVALUATION_QUEUED
```

---

This prevents:

```text id="0s5r2r"
Duplicate Fraud Evaluations

Race Conditions

Inconsistent Fraud Scores
```

---

# 15. Recovery Service

Responsibilities:

```text id="6f2x9v"
Detect Stuck Jobs

Reset Jobs

Generate Recovery Events
```

---

## Stuck Job Definition

```text id="k8b1za"
PROCESSING

For More Than

30 Minutes
```

---

## Recovery Workflow

```text id="up1ll7"
PROCESSING
      ↓
Timeout
      ↓
Recovery Worker
      ↓
PENDING
      ↓
PROCESSING_RECOVERED
```

---

# 16. Worker Design

## OCR Worker

Uses:

```sql id="q7r39k"
FOR UPDATE SKIP LOCKED
```

---

## Analysis Worker

Uses:

```sql id="xy7e9m"
FOR UPDATE SKIP LOCKED
```

---

## Benefits

```text id="5v10l6"
Multiple Workers

No Duplicate Processing

Crash Recovery
```

---

# 17. Repository Layer

## OcrJobRepository

Methods:

```java id="h6x3pk"
findPendingJobs()

findByClaimId()

findByDocumentVersionId()
```

---

## AnalysisJobRepository

Methods:

```java id="s4a6f0"
findPendingJobs()

findByClaimId()

findByDocumentVersionId()
```

---

## ProcessingStateRepository

Methods:

```java id="x5lyw8"
findByClaimId()
```

---

# 18. Event Publishing

## ProcessingEventPublisher

Events:

```text id="abv3v5"
OCR_JOB_CREATED

ANALYSIS_JOB_CREATED

CLAIM_PROCESSING_COMPLETED

FRAUD_EVALUATION_QUEUED

PROCESSING_RECOVERED
```

---

All events use:

```text id="30n5m2"
Outbox Pattern
```

---

# 19. Transaction Boundaries

## Create OCR Job

```java id="qjm6f0"
@Transactional
```

Workflow:

```text id="xch4xg"
Create Job
      ↓
Persist Job
      ↓
Save Event
      ↓
Commit
```

---

## Complete Analysis

```java id="a7j9qo"
@Transactional
```

Workflow:

```text id="g4cax4"
Store Result
      ↓
Update State
      ↓
Check Completion
      ↓
Save Event
      ↓
Commit
```

---

# 20. Exception Handling

Exceptions:

```java id="ld1j20"
OcrJobNotFoundException

AnalysisJobNotFoundException

ProcessingStateException

ProcessingRecoveryException
```

---

# 21. Security Rules

Required Permissions:

```text id="i4p56i"
PROCESSING_VIEW

PROCESSING_RETRY

PROCESSING_REPROCESS
```

---

Tenant Isolation:

```text id="x3m4m8"
All Queries Filtered
By tenant_id
```

---

# 22. Audit Integration

Tracked:

```text id="8ap7dg"
OCR Job Created

OCR Completed

Analysis Completed

Processing Completed

Fraud Queued

Recovery Executed
```

---

Every action creates:

```text id="w7v7n7"
AUDIT_EVENT_CREATED
```

---

# 23. Performance Considerations

Indexes:

```text id="3z3jj6"
claim_id

document_version_id

job_status

tenant_id
```

---

Worker Polling:

```text id="gsh6wf"
Every 10 Seconds
```

---

# 24. Dependency Diagram

```text id="mxmdp7"
Document Events
        ↓
ProcessingOrchestrator
        ↓
OcrJobService
        ↓
OCR Service

OCR_COMPLETED
        ↓
AnalysisJobService
        ↓
Analysis Service

Analysis Completed
        ↓
ProcessingStateService
        ↓
Claim Processing Complete
        ↓
Fraud Queue
```

---

# 25. Unit Testing Requirements

Coverage Target:

```text id="qzazc5"
90%+
```

Required Tests:

```text id="4c0byc"
OCR Job Creation

Analysis Job Creation

Processing Completion

Fraud Queue Trigger

Incremental Reprocessing

Recovery Workflow

Worker Locking Logic
```

---

# 26. Future Enhancements

V2 Reserved:

```text id="80dn9m"
Kafka Integration

Parallel Processing

Workflow Engine

Dynamic Processing Pipelines

AI Document Understanding
```

---

# Approval

This document defines the Processing Module implementation blueprint and serves as the reference for OCR orchestration, analysis orchestration, workflow coordination, recovery handling, incremental reprocessing, fraud trigger management, event publishing, transaction management, audit integration, and future scalability.
