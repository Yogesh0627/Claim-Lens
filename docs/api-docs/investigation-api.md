# 07.9 Investigation Management API

## Document Information

| Field           | Value                        |
| --------------- | ---------------------------- |
| Project         | ClaimLens                    |
| Company         | RiskLens Technologies        |
| Version         | V1                           |
| Document Type   | API Design                   |
| Document Number | 07.9                         |
| Document Name   | Investigation Management API |
| Status          | Approved                     |
| Last Updated    | June 2026                    |

---

# 1. Overview

The Investigation Management module manages the operational investigation process after a claim has been assigned to an investigator.

This module enables investigators to:

* Create Investigations
* Manage Investigation Lifecycle
* Record Investigation Notes
* Manage Investigation Tasks
* Upload Findings
* Create Investigation Reports
* Track Evidence
* Maintain Investigation Timeline
* Submit Final Recommendations

The Investigation module is the primary workspace used by investigators.

---

# 2. Domain Entities

Managed Entities:

```text
investigation
investigation_note
investigation_task
investigation_report
```

Related Entities:

```text
claim
assignment
document
fraud_score
fraud_alert
user
```

---

# 3. Investigation Lifecycle

## Investigation Statuses

```text
OPEN
IN_PROGRESS
WAITING_FOR_INFORMATION
UNDER_REVIEW
COMPLETED
CLOSED
```

---

## Investigation Flow

```text
Assignment Created
        ↓
Investigation Created
        ↓
OPEN
        ↓
IN_PROGRESS
        ↓
WAITING_FOR_INFORMATION
        ↓
UNDER_REVIEW
        ↓
COMPLETED
        ↓
CLOSED
```

---

# 4. Permission Matrix

| Permission                  | Description           |
| --------------------------- | --------------------- |
| INVESTIGATION_VIEW          | View investigations   |
| INVESTIGATION_CREATE        | Create investigations |
| INVESTIGATION_UPDATE        | Update investigations |
| INVESTIGATION_CLOSE         | Close investigations  |
| INVESTIGATION_NOTE_CREATE   | Create notes          |
| INVESTIGATION_TASK_MANAGE   | Manage tasks          |
| INVESTIGATION_REPORT_CREATE | Create reports        |

---

# 5. Investigation APIs

---

## Create Investigation

### Endpoint

```http
POST /api/v1/investigations
```

### Permissions

```text
INVESTIGATION_CREATE
```

### Request Body

```json
{
  "claimId": 10001,
  "assignmentId": 5001,
  "priority": "HIGH"
}
```

### Business Rules

* Claim must exist.
* Assignment must exist.
* Only one active investigation per claim.
* Investigator must own assignment.

### Success Response

```json
{
  "investigationId": 9001,
  "investigationNumber": "INV-2026-000001",
  "status": "OPEN"
}
```

### Audit Event

```text
INVESTIGATION_CREATED
```

---

## Get Investigation

### Endpoint

```http
GET /api/v1/investigations/{investigationId}
```

### Success Response

```json
{
  "investigationId": 9001,
  "investigationNumber": "INV-2026-000001",
  "claimId": 10001,
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "createdAt": "2026-06-01T10:00:00Z"
}
```

---

## Update Investigation

### Endpoint

```http
PUT /api/v1/investigations/{investigationId}
```

### Request Body

```json
{
  "priority": "MEDIUM"
}
```

### Success Response

```json
{
  "message": "Investigation updated successfully"
}
```

### Audit Event

```text
INVESTIGATION_UPDATED
```

---

## List Investigations

### Endpoint

```http
GET /api/v1/investigations
```

### Query Parameters

```text
page
size
status
claimId
investigatorId
priority
```

### Success Response

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

---

# 6. Investigation Status APIs

---

## Update Investigation Status

### Endpoint

```http
PATCH /api/v1/investigations/{investigationId}/status
```

### Request Body

```json
{
  "status": "IN_PROGRESS",
  "reason": "Document review started"
}
```

### Success Response

```json
{
  "status": "IN_PROGRESS"
}
```

### Audit Event

```text
INVESTIGATION_STATUS_CHANGED
```

---

## Get Allowed Status Transitions

### Endpoint

```http
GET /api/v1/investigations/{investigationId}/allowed-statuses
```

### Success Response

```json
[
  "WAITING_FOR_INFORMATION",
  "UNDER_REVIEW"
]
```

---

# 7. Investigation Notes APIs

---

## Create Investigation Note

### Endpoint

```http
POST /api/v1/investigations/{investigationId}/notes
```

### Request Body

```json
{
  "note": "Vehicle damage photographs verified."
}
```

### Success Response

```json
{
  "noteId": 1001
}
```

### Audit Event

```text
INVESTIGATION_NOTE_CREATED
```

---

## Get Investigation Notes

### Endpoint

```http
GET /api/v1/investigations/{investigationId}/notes
```

### Success Response

```json
[
  {
    "noteId": 1001,
    "note": "Vehicle damage photographs verified.",
    "createdBy": "EMP0001",
    "createdAt": "2026-06-01T10:30:00Z"
  }
]
```

---

## Update Investigation Note

### Endpoint

```http
PUT /api/v1/investigations/{investigationId}/notes/{noteId}
```

### Success Response

```json
{
  "message": "Note updated successfully"
}
```

### Audit Event

```text
INVESTIGATION_NOTE_UPDATED
```

---

# 8. Investigation Task APIs

---

## Create Investigation Task

### Endpoint

```http
POST /api/v1/investigations/{investigationId}/tasks
```

### Request Body

```json
{
  "title": "Verify Vehicle Ownership",
  "description": "Validate RC ownership records",
  "dueDate": "2026-06-05"
}
```

### Success Response

```json
{
  "taskId": 2001
}
```

### Audit Event

```text
INVESTIGATION_TASK_CREATED
```

---

## Get Investigation Tasks

### Endpoint

```http
GET /api/v1/investigations/{investigationId}/tasks
```

### Success Response

```json
[
  {
    "taskId": 2001,
    "title": "Verify Vehicle Ownership",
    "status": "OPEN"
  }
]
```

---

## Update Task Status

### Endpoint

```http
PATCH /api/v1/investigations/{investigationId}/tasks/{taskId}/status
```

### Request Body

```json
{
  "status": "COMPLETED"
}
```

### Success Response

```json
{
  "status": "COMPLETED"
}
```

### Audit Event

```text
INVESTIGATION_TASK_COMPLETED
```

---

## Delete Task

### Endpoint

```http
DELETE /api/v1/investigations/{investigationId}/tasks/{taskId}
```

### Success Response

```http
204 No Content
```

### Audit Event

```text
INVESTIGATION_TASK_DELETED
```

---

# 9. Evidence APIs

---

## Link Evidence Document

### Endpoint

```http
POST /api/v1/investigations/{investigationId}/evidence
```

### Request Body

```json
{
  "documentId": 2001
}
```

### Success Response

```json
{
  "message": "Evidence linked successfully"
}
```

### Audit Event

```text
EVIDENCE_LINKED
```

---

## Get Investigation Evidence

### Endpoint

```http
GET /api/v1/investigations/{investigationId}/evidence
```

### Success Response

```json
[
  {
    "documentId": 2001,
    "documentType": "VEHICLE_RC"
  }
]
```

---

# 10. Investigation Report APIs

---

## Create Investigation Report

### Endpoint

```http
POST /api/v1/investigations/{investigationId}/reports
```

### Headers

```http
Idempotency-Key: uuid
```

### Request Body

```json
{
  "summary": "Vehicle ownership verified.",
  "findings": "No discrepancies found.",
  "recommendation": "APPROVE"
}
```

### Success Response

```json
{
  "reportId": 3001
}
```

### Audit Event

```text
INVESTIGATION_REPORT_CREATED
```

---

## Get Investigation Report

### Endpoint

```http
GET /api/v1/investigations/{investigationId}/reports/{reportId}
```

### Success Response

```json
{
  "reportId": 3001,
  "summary": "Vehicle ownership verified.",
  "recommendation": "APPROVE"
}
```

---

## Update Investigation Report

### Endpoint

```http
PUT /api/v1/investigations/{investigationId}/reports/{reportId}
```

### Success Response

```json
{
  "message": "Report updated successfully"
}
```

### Audit Event

```text
INVESTIGATION_REPORT_UPDATED
```

---

# 11. Investigation Timeline APIs

---

## Get Investigation Timeline

### Endpoint

```http
GET /api/v1/investigations/{investigationId}/timeline
```

### Description

Unified timeline containing:

* Status Changes
* Notes
* Tasks
* Evidence
* Reports

### Success Response

```json
[
  {
    "eventType": "INVESTIGATION_CREATED",
    "timestamp": "2026-06-01T10:00:00Z"
  },
  {
    "eventType": "TASK_COMPLETED",
    "timestamp": "2026-06-02T14:00:00Z"
  }
]
```

---

# 12. Investigation Closure APIs

---

## Complete Investigation

### Endpoint

```http
POST /api/v1/investigations/{investigationId}/complete
```

### Request Body

```json
{
  "completionNotes": "Investigation completed successfully."
}
```

### Business Rules

* All mandatory tasks completed.
* Final report exists.

### Success Response

```json
{
  "status": "COMPLETED"
}
```

### Audit Event

```text
INVESTIGATION_COMPLETED
```

---

## Close Investigation

### Endpoint

```http
POST /api/v1/investigations/{investigationId}/close
```

### Request Body

```json
{
  "closureReason": "Claim approved"
}
```

### Success Response

```json
{
  "status": "CLOSED"
}
```

### Audit Event

```text
INVESTIGATION_CLOSED
```

---

# 13. Investigation Summary API

---

## Get Investigation Summary

### Endpoint

```http
GET /api/v1/investigations/{investigationId}/summary
```

### Success Response

```json
{
  "investigationId": 9001,
  "status": "IN_PROGRESS",
  "taskCount": 8,
  "completedTasks": 5,
  "noteCount": 12,
  "evidenceCount": 6,
  "reportStatus": "DRAFT"
}
```

---

# 14. Business Validation Rules

## Investigation Creation

* One active investigation per claim.
* Assignment required.
* Investigator ownership validation required.

## Notes

* Notes immutable after investigation closure.
* Notes tracked in audit log.

## Tasks

* Closed investigations cannot create tasks.
* Due dates cannot be in the past.

## Reports

* Only assigned investigator may submit report.
* Final report required before completion.

## Closure

* Investigation must be completed before closure.
* Closure reason mandatory.

---

# 15. Error Codes

| Error Code                   | Description                 |
| ---------------------------- | --------------------------- |
| INVESTIGATION_NOT_FOUND      | Investigation missing       |
| INVESTIGATION_ALREADY_EXISTS | Active investigation exists |
| INVESTIGATION_CLOSED         | Investigation closed        |
| NOTE_NOT_FOUND               | Note missing                |
| TASK_NOT_FOUND               | Task missing                |
| REPORT_NOT_FOUND             | Report missing              |
| INVALID_STATUS_TRANSITION    | Invalid status change       |
| FINAL_REPORT_REQUIRED        | Report required             |
| OPEN_TASKS_EXIST             | Open tasks remain           |
| EVIDENCE_NOT_FOUND           | Evidence missing            |

---

# 16. Audit Events

```text
INVESTIGATION_CREATED
INVESTIGATION_UPDATED
INVESTIGATION_STATUS_CHANGED

INVESTIGATION_NOTE_CREATED
INVESTIGATION_NOTE_UPDATED

INVESTIGATION_TASK_CREATED
INVESTIGATION_TASK_COMPLETED
INVESTIGATION_TASK_DELETED

EVIDENCE_LINKED

INVESTIGATION_REPORT_CREATED
INVESTIGATION_REPORT_UPDATED

INVESTIGATION_COMPLETED
INVESTIGATION_CLOSED
```

---

# 17. Integration Events

Produced Events:

```text
INVESTIGATION_CREATED
INVESTIGATION_COMPLETED
INVESTIGATION_CLOSED
REPORT_SUBMITTED
```

Consumed By:

```text
CLAIM MODULE
ASSIGNMENT MODULE
FRAUD MODULE
NOTIFICATION MODULE
ANALYTICS MODULE
```

---

# 18. Performance Requirements

| API                  | Target   |
| -------------------- | -------- |
| Create Investigation | < 500 ms |
| Add Note             | < 200 ms |
| Create Task          | < 200 ms |
| Create Report        | < 500 ms |
| Timeline Retrieval   | < 500 ms |
| Summary Retrieval    | < 200 ms |

---

# 19. Security Requirements

All APIs require:

```text
JWT Authentication
Tenant Validation
Permission Validation
Audit Logging
```

Additional Controls:

* Investigators access only assigned investigations.
* Managers access investigations within scope.
* Reports protected from unauthorized modification.
* Cross-tenant access prohibited.

---

# 20. Investigation Workspace Integration

The Investigation Workspace UI consumes:

```text
GET /investigations/{id}
GET /investigations/{id}/tasks
GET /investigations/{id}/notes
GET /investigations/{id}/evidence
GET /investigations/{id}/timeline
GET /investigations/{id}/summary
```

to build the investigator's operational dashboard.

---

# Approval

This document defines the Investigation Management API contract for ClaimLens and serves as the implementation reference for investigation workflows, notes, tasks, evidence management, reporting, timelines, findings, and investigation closure processes.
