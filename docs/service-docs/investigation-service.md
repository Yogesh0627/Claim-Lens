> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; the authoritative behaviour is the service code. Where this diverges, the code wins ([`../architecture.md`](../architecture.md), [`../domain-model.md`](../domain-model.md), [`../audit-report.md`](../audit-report.md)); deltas flagged inline as **As-built** notes.

# 09.5 Investigation Service Design

## Document Information

| Field           | Value                        |
| --------------- | ---------------------------- |
| Project         | ClaimLens                    |
| Company         | Niyo Technologies        |
| Version         | V1                           |
| Document Type   | Service Design               |
| Document Number | 09.5                         |
| Document Name   | Investigation Service Design |
| Status          | Approved                     |
| Last Updated    | June 2026                    |

---

# 1. Overview

The Investigation Module manages fraud investigations initiated from fraud alerts.

Responsibilities:

* Investigation Lifecycle Management
* Investigation Notes
* Investigation Tasks
* Investigation Reports
* Evidence Tracking
* Investigation Completion
* Fraud Review Support

The Investigation Module serves as the primary workspace for investigators.

**As-built (2026-07-29):** the module was reduced to a **single append-only `InvestigationNote`**. There is **no** `Investigation` aggregate, `InvestigationTask`, `InvestigationReport`, `InvestigationFinding` or `InvestigationEvidence` entity (all dropped), no investigation status lifecycle, and no `InvestigationController` — notes are served under the claim at **`POST/GET /claims/{id}/investigation-notes`**. `addNote` (`CLAIM_INVESTIGATE`) requires the claim to be **UNDER_INVESTIGATION or WAITING_FOR_CUSTOMER**; `listNotes` uses `CLAIM_READ`. The investigation state a claim moves through is carried by the **claim status** itself, not a separate Investigation entity.

---

# 2. Module Dependencies

## Consumed Events

```text
ASSIGNMENT_CREATED

ASSIGNMENT_REASSIGNED

FRAUD_ALERT_CREATED
```

---

## Published Events

```text
INVESTIGATION_CREATED

INVESTIGATION_STARTED

INVESTIGATION_STATUS_CHANGED

INVESTIGATION_NOTE_ADDED

INVESTIGATION_TASK_CREATED

INVESTIGATION_TASK_COMPLETED

INVESTIGATION_REPORT_SUBMITTED

INVESTIGATION_COMPLETED

INVESTIGATION_CLOSED
```

---

# 3. Package Structure

```text
investigation

├── controller
│   └── InvestigationController

├── service
│   ├── InvestigationService
│   ├── InvestigationTaskService
│   ├── InvestigationReportService
│   └── InvestigationServiceImpl

├── repository
│   ├── InvestigationRepository
│   ├── InvestigationNoteRepository
│   ├── InvestigationTaskRepository
│   └── InvestigationReportRepository

├── entity
│   ├── Investigation
│   ├── InvestigationNote
│   ├── InvestigationTask
│   └── InvestigationReport

├── dto

├── mapper

├── validator

├── event

├── exception
```

---

# 4. Domain Entities

Primary Aggregate:

```text
Investigation
```

Supporting Entities:

```text
InvestigationNote

InvestigationTask

InvestigationReport
```

---

# 5. Controller Layer

## InvestigationController

Base Path

```http
/api/v1/investigations
```

---

## Endpoints

```http
GET    /investigations

GET    /investigations/{investigationId}

POST   /investigations/{investigationId}/start

PATCH  /investigations/{investigationId}/status

POST   /investigations/{investigationId}/notes

POST   /investigations/{investigationId}/tasks

PATCH  /investigations/tasks/{taskId}/complete

POST   /investigations/{investigationId}/report

POST   /investigations/{investigationId}/complete

POST   /investigations/{investigationId}/close
```

**As-built (2026-07-29):** the entire `/investigations` surface was **not built**. Only two endpoints exist, both nested under the claim: `POST /claims/{id}/investigation-notes` (`CLAIM_INVESTIGATE`) and `GET /claims/{id}/investigation-notes` (`CLAIM_READ`). Sections §6–§8, §10, §12, §14, §15 (tasks, reports, status lifecycle, task/report services) therefore describe unbuilt behaviour.

---

# 6. Service Layer

## InvestigationService

```java
public interface InvestigationService
```

---

## Methods

### Create Investigation

```java
InvestigationResponse createInvestigation(
    CreateInvestigationRequest request
);
```

---

### Start Investigation

```java
void startInvestigation(
    Long investigationId
);
```

---

### Update Status

```java
void updateStatus(
    Long investigationId,
    UpdateInvestigationStatusRequest request
);
```

---

### Complete Investigation

```java
void completeInvestigation(
    Long investigationId
);
```

---

### Close Investigation

```java
void closeInvestigation(
    Long investigationId
);
```

---

# 7. Investigation Task Service

Responsibilities:

```text
Create Tasks

Assign Tasks

Complete Tasks

Track Task Status
```

---

## Methods

```java
createTask()

completeTask()

getTasksByInvestigation()
```

---

# 8. Investigation Report Service

Responsibilities:

```text
Create Report

Update Draft

Submit Report

Finalize Findings
```

---

## Methods

```java
saveDraft()

submitReport()

getReport()
```

---

# 9. DTO Design

## CreateInvestigationRequest

```java
claimId

assignmentId

fraudAlertId

investigatorId
```

---

## AddNoteRequest

```java
note
```

---

## CreateTaskRequest

```java
title

description

dueDate
```

---

## SubmitReportRequest

```java
summary

findings

recommendation
```

---

## InvestigationResponse

```java
investigationId

investigationNumber

claimId

investigatorId

status

createdAt
```

---

# 10. Investigation Status Lifecycle

Allowed States:

```text
OPEN

IN_PROGRESS

WAITING_FOR_INFORMATION

UNDER_REVIEW

COMPLETED

CLOSED
```

---

## Valid Transitions

```text
OPEN
  ↓
IN_PROGRESS
  ↓
UNDER_REVIEW
  ↓
COMPLETED
  ↓
CLOSED
```

---

# 11. Repository Layer

## InvestigationRepository

```java
extends JpaRepository<
    Investigation,
    Long
>
```

---

## Methods

```java
Optional<Investigation>
findByClaimId(Long claimId);
```

---

```java
List<Investigation>
findByInvestigatorId(Long userId);
```

---

## InvestigationTaskRepository

```java
List<InvestigationTask>
findByInvestigationId(
    Long investigationId
);
```

---

# 12. Validation Layer

## InvestigationValidator

Responsibilities:

```text
Status Validation

Task Validation

Report Validation

Assignment Validation
```

---

## Example

Allowed:

```text
IN_PROGRESS
    ↓
UNDER_REVIEW
```

Forbidden:

```text
CLOSED
    ↓
IN_PROGRESS
```

---

# 13. Investigation Notes Strategy

Notes are:

```text
Append Only
```

---

Rules:

```text
Cannot Edit

Cannot Delete

Fully Auditable
```

**As-built (2026-07-29):** accurate — notes are insert-only (`InvestigationNote`). Each note carries `noteType`, free-text `note`, optional `severity`, and an optional linked `documentId`; creation is audited via `@Auditable(action="INVESTIGATION_NOTE_ADDED")`.

---

# 14. Investigation Task Strategy

Task Statuses:

```text
OPEN

IN_PROGRESS

COMPLETED
```

---

Completion Rules:

```text
Mandatory Tasks
Must Be Completed
Before Investigation Completion
```

---

# 15. Investigation Report Strategy

One Final Report Per Investigation.

---

Draft Allowed:

```text
Multiple Updates
```

---

Final Submission:

```text
Single Submission
```

After submission:

```text
Read Only
```

---

# 16. Event Publishing

## InvestigationEventPublisher

Published Events:

```text
INVESTIGATION_CREATED

INVESTIGATION_STARTED

INVESTIGATION_STATUS_CHANGED

INVESTIGATION_NOTE_ADDED

INVESTIGATION_TASK_CREATED

INVESTIGATION_TASK_COMPLETED

INVESTIGATION_REPORT_SUBMITTED

INVESTIGATION_COMPLETED

INVESTIGATION_CLOSED
```

---

All events use:

```text
Outbox Pattern
```

---

# 17. Transaction Boundaries

## Start Investigation

```java
@Transactional
```

Workflow:

```text
Validate
     ↓
Update Status
     ↓
Save Event
     ↓
Commit
```

---

## Submit Report

```java
@Transactional
```

Workflow:

```text
Validate Report
       ↓
Persist Report
       ↓
Save Event
       ↓
Commit
```

---

## Complete Investigation

```java
@Transactional
```

Workflow:

```text
Validate Tasks
       ↓
Validate Report
       ↓
Update Status
       ↓
Save Event
       ↓
Commit
```

---

# 18. Exception Handling

Exceptions:

```java
InvestigationNotFoundException

InvalidInvestigationStateException

MandatoryTasksIncompleteException

ReportNotSubmittedException
```

---

# 19. Security Rules

Required Permissions:

```text
INVESTIGATION_VIEW

INVESTIGATION_UPDATE

INVESTIGATION_COMPLETE

INVESTIGATION_CLOSE
```

**As-built (2026-07-29):** no `INVESTIGATION_*` permissions exist. Adding a note requires **`CLAIM_INVESTIGATE`**, reading requires **`CLAIM_READ`**, enforced via `@PreAuthorize` on the impl. Tenant isolation is automatic via `@TenantId`.

---

Tenant Isolation:

```text
All Queries Filtered
By tenant_id
```

---

# 20. Audit Integration

Tracked Actions:

```text
Investigation Created

Investigation Started

Note Added

Task Created

Task Completed

Report Submitted

Investigation Completed

Investigation Closed
```

---

Every write operation creates:

```text
AUDIT_EVENT_CREATED
```

---

# 21. Performance Considerations

Indexes:

```text
claim_id

assignment_id

investigator_id

status

tenant_id
```

---

Pagination Mandatory For:

```text
Investigation Search

Task Search

Notes Listing
```

---

# 22. Dependency Diagram

```text
InvestigationController
            ↓
InvestigationService
            ↓
InvestigationValidator
            ↓
Repositories
            ↓
PostgreSQL

InvestigationService
            ↓
InvestigationEventPublisher
            ↓
Outbox Table
```

---

# 23. Unit Testing Requirements

Coverage Target:

```text
90%+
```

Required Tests:

```text
Create Investigation

Start Investigation

Status Changes

Add Note

Create Task

Complete Task

Submit Report

Complete Investigation

Close Investigation
```

---

# 24. Future Enhancements

V2 Reserved:

```text
Evidence Management

Photo Evidence Review

Investigation Templates

AI Investigation Summaries

Collaborative Investigations
```

---

# Approval

This document defines the Investigation Module implementation blueprint and serves as the reference for investigation lifecycle management, task tracking, report submission, fraud review workflows, event publishing, transaction management, audit integration, and future scalability.
