> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; the authoritative behaviour is the code. Where this diverges, the code wins ([`../architecture.md`](../architecture.md), [`../domain-model.md`](../domain-model.md)); deltas flagged inline as **As-built** notes.

# 08.5 Investigation Events

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | Event Design          |
| Document Number | 08.5                  |
| Document Name   | Investigation Events  |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

This document defines all events produced by the Investigation Module.

Investigation Events track the complete lifecycle of claim investigations.

> **As-built (2026-07-29):** The rich investigation lifecycle described here was **not built**, and none of these events are emitted. There is no `Investigation` entity, no investigation number, and no tasks/reports/evidence — `InvestigationFinding` and `InvestigationEvidence` were dropped. In the code, "investigation" is simply a **claim in `UNDER_INVESTIGATION` status** after it is assigned to an investigator (a `claim_assignment` row); the investigator may add free-text `investigation_note` rows and then resolves the claim via `decide()` (APPROVE/REJECT) or `requestInformation()`. So `INVESTIGATION_CREATED/STARTED/STATUS_CHANGED/TASK_*/REPORT_SUBMITTED/COMPLETED/CLOSED` have no counterpart in the build.

These events support:

* Investigation Workflows
* Task Tracking
* Fraud Review Workflows
* SLA Monitoring
* Investigator Performance Analytics
* Notifications
* Audit Logging

---

# 2. Investigation Event Catalog

| Event                          | Category             |
| ------------------------------ | -------------------- |
| INVESTIGATION_CREATED          | Domain + Integration |
| INVESTIGATION_STARTED          | Domain               |
| INVESTIGATION_STATUS_CHANGED   | Domain + Integration |
| INVESTIGATION_NOTE_ADDED       | Domain               |
| INVESTIGATION_TASK_CREATED     | Domain               |
| INVESTIGATION_TASK_COMPLETED   | Domain               |
| INVESTIGATION_REPORT_SUBMITTED | Integration          |
| INVESTIGATION_COMPLETED        | Domain + Integration |
| INVESTIGATION_CLOSED           | Domain + Integration |

---

# 3. INVESTIGATION_CREATED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Investigation automatically created after assignment.

---

## Producer

```text
Investigation Module
```

---

## Consumers

```text
Notification Module
Analytics Module
Audit Module
```

---

## Source Event

```text
ASSIGNMENT_CREATED
```

---

## Payload

```json
{
  "investigationId": 9001,
  "investigationNumber": "INV-2026-000001",
  "claimId": 10001,
  "assignmentId": 5001,
  "investigatorId": 101
}
```

---

## Business Rules

* One active investigation per claim.
* Assignment required.
* Investigation number generated.

---

# 4. INVESTIGATION_STARTED

## Category

```text
Domain Event
```

---

## Trigger

Investigator begins working on the case.

---

## Producer

```text
Investigation Module
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
  "investigationId": 9001,
  "startedBy": 101,
  "startedAt": "2026-06-01T10:00:00Z"
}
```

---

# 5. INVESTIGATION_STATUS_CHANGED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Investigation state changes.

---

## Valid Statuses

```text
OPEN
IN_PROGRESS
WAITING_FOR_INFORMATION
UNDER_REVIEW
COMPLETED
CLOSED
```

---

## Producer

```text
Investigation Module
```

---

## Consumers

```text
Claim Module
Notification Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "investigationId": 9001,
  "claimId": 10001,
  "oldStatus": "OPEN",
  "newStatus": "IN_PROGRESS"
}
```

---

# 6. INVESTIGATION_NOTE_ADDED

## Category

```text
Domain Event
```

---

## Trigger

Investigator adds a note.

---

## Producer

```text
Investigation Module
```

---

## Consumers

```text
Audit Module
Analytics Module
```

---

## Payload

```json
{
  "investigationId": 9001,
  "noteId": 4001,
  "addedBy": 101
}
```

---

# 7. INVESTIGATION_TASK_CREATED

## Category

```text
Domain Event
```

---

## Trigger

Investigation task created.

---

## Producer

```text
Investigation Module
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
  "investigationId": 9001,
  "taskId": 7001,
  "title": "Verify Vehicle Ownership"
}
```

---

# 8. INVESTIGATION_TASK_COMPLETED

## Category

```text
Domain Event
```

---

## Trigger

Investigation task completed.

---

## Producer

```text
Investigation Module
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
  "investigationId": 9001,
  "taskId": 7001,
  "completedBy": 101
}
```

---

# 9. INVESTIGATION_REPORT_SUBMITTED

## Category

```text
Integration Event
```

---

## Trigger

Final investigation report submitted.

---

## Producer

```text
Investigation Module
```

---

## Consumers

```text
Fraud Module
Claim Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "investigationId": 9001,
  "reportId": 3001,
  "claimId": 10001,
  "recommendation": "APPROVE"
}
```

---

## Business Rules

* Final report required.
* Submitted once per investigation.

---

# 10. INVESTIGATION_COMPLETED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Investigation completed successfully.

---

## Producer

```text
Investigation Module
```

---

## Consumers

```text
Assignment Module
Claim Module
Fraud Module
Analytics Module
Audit Module
Notification Module
```

---

## Payload

```json
{
  "investigationId": 9001,
  "claimId": 10001,
  "completedAt": "2026-06-10T15:00:00Z"
}
```

---

## Business Rules

Required:

```text
Final Report Exists
Mandatory Tasks Complete
```

---

# 11. INVESTIGATION_CLOSED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Investigation formally closed.

---

## Producer

```text
Investigation Module
```

---

## Consumers

```text
Claim Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "investigationId": 9001,
  "claimId": 10001,
  "closedAt": "2026-06-10T16:00:00Z",
  "closureReason": "Claim Approved"
}
```

---

# 12. Investigation Workflow

## Standard Investigation Flow

```text
ASSIGNMENT_CREATED
          ↓
INVESTIGATION_CREATED
          ↓
INVESTIGATION_STARTED
          ↓
INVESTIGATION_STATUS_CHANGED
          ↓
INVESTIGATION_TASK_CREATED
          ↓
INVESTIGATION_TASK_COMPLETED
          ↓
INVESTIGATION_REPORT_SUBMITTED
          ↓
INVESTIGATION_COMPLETED
          ↓
INVESTIGATION_CLOSED
```

---

# 13. Fraud Review Workflow

```text
FRAUD_ALERT_CREATED
          ↓
ASSIGNMENT_CREATED
          ↓
INVESTIGATION_CREATED
          ↓
INVESTIGATION_REPORT_SUBMITTED
          ↓
FRAUD_REVIEW_COMPLETED
```

---

# 14. Correlation Strategy

All investigation events share:

```text
claim workflow correlationId
```

Example:

```text
CLAIM_CREATED
DOCUMENT_UPLOADED
OCR_COMPLETED
ANALYSIS_COMPLETED
FRAUD_EVALUATED
ASSIGNMENT_CREATED
INVESTIGATION_COMPLETED
```

---

# 15. Business Validation Rules

## Investigation Creation

* One active investigation per claim.
* Assignment mandatory.

## Completion

* Final report required.
* Mandatory tasks completed.

## Closure

* Investigation must be completed.
* Closure reason mandatory.

---

# 16. Monitoring Metrics

```text
investigation_created_total

investigation_completed_total

investigation_closed_total

investigation_tasks_completed_total

average_investigation_duration
```

---

# 17. Security Requirements

Event payloads must never contain:

```text
Investigation Notes Content
Customer PII
Internal Security Findings
Authentication Data
```

Only identifiers and metadata allowed.

---

# 18. Future Events (V2)

Reserved:

```text
INVESTIGATION_EVIDENCE_ADDED

INVESTIGATION_EVIDENCE_VERIFIED

INVESTIGATION_REOPENED

INVESTIGATION_ESCALATED
```

---

# Approval

This document defines the Investigation Domain Event Catalog for ClaimLens and serves as the implementation reference for investigation lifecycle management, task tracking, report submission, fraud review workflows, analytics aggregation, notifications, and audit tracking.
