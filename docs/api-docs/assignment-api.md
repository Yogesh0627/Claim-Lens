# 07.8 Assignment Management API

## Document Information

| Field           | Value                     |
| --------------- | ------------------------- |
| Project         | ClaimLens                 |
| Company         | RiskLens Technologies     |
| Version         | V1                        |
| Document Type   | API Design                |
| Document Number | 07.8                      |
| Document Name   | Assignment Management API |
| Status          | Approved                  |
| Last Updated    | June 2026                 |

---

# 1. Overview

The Assignment Management module is responsible for assigning claims to investigators.

The module supports:

* Automatic Assignment
* Manual Assignment
* Claim Reassignment
* Assignment Queue Management
* Workload Distribution
* Assignment History Tracking
* Investigator Availability Management
* Assignment Policy Integration

The Assignment Engine uses policies configured in the Policy Engine to determine investigator allocation.

---

# 2. Domain Entities

Managed Entities:

```text
assignment
assignment_history
assignment_queue
```

Related Entities:

```text
claim
user
assignment_policy
branch
region
```

---

# 3. Assignment Architecture

## Automatic Assignment Flow

```text
Claim Submitted
      ↓
Assignment Queue
      ↓
Assignment Engine
      ↓
Policy Evaluation
      ↓
Investigator Selection
      ↓
Assignment Created
      ↓
Notification Sent
```

---

## Manual Assignment Flow

```text
Claims Manager
      ↓
Select Claim
      ↓
Select Investigator
      ↓
Assignment Created
      ↓
History Recorded
```

---

# 4. Assignment Status Lifecycle

Supported Statuses:

```text
QUEUED
ASSIGNED
ACCEPTED
REJECTED
REASSIGNED
COMPLETED
CANCELLED
```

---

# 5. Permission Matrix

| Permission              | Description           |
| ----------------------- | --------------------- |
| ASSIGNMENT_VIEW         | View assignments      |
| ASSIGNMENT_CREATE       | Create assignments    |
| ASSIGNMENT_UPDATE       | Update assignments    |
| ASSIGNMENT_REASSIGN     | Reassign claims       |
| ASSIGNMENT_QUEUE_VIEW   | View assignment queue |
| ASSIGNMENT_QUEUE_MANAGE | Manage queue          |

---

# 6. Assignment APIs

---

## Create Manual Assignment

### Endpoint

```http
POST /api/v1/assignments
```

### Permissions

```text
ASSIGNMENT_CREATE
```

### Request Body

```json
{
  "claimId": 10001,
  "investigatorId": 1001,
  "assignmentReason": "Manual escalation by manager"
}
```

### Business Rules

* Claim must exist.
* Investigator must be active.
* Claim cannot already have an active assignment.
* Investigator must belong to tenant.

### Success Response

```http
201 Created
```

```json
{
  "assignmentId": 5001,
  "claimId": 10001,
  "investigatorId": 1001,
  "status": "ASSIGNED"
}
```

### Audit Event

```text
ASSIGNMENT_CREATED
```

---

## Get Assignment

### Endpoint

```http
GET /api/v1/assignments/{assignmentId}
```

### Permissions

```text
ASSIGNMENT_VIEW
```

### Success Response

```json
{
  "assignmentId": 5001,
  "claimId": 10001,
  "investigatorId": 1001,
  "status": "ASSIGNED",
  "assignedAt": "2026-06-01T10:00:00Z"
}
```

---

## List Assignments

### Endpoint

```http
GET /api/v1/assignments
```

### Query Parameters

```text
page
size
status
claimId
investigatorId
createdFrom
createdTo
```

### Example

```http
GET /api/v1/assignments?investigatorId=1001
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

# 7. Automatic Assignment APIs

---

## Trigger Assignment

### Endpoint

```http
POST /api/v1/assignments/auto-assign
```

### Description

Manually trigger assignment engine for a queued claim.

### Request Body

```json
{
  "claimId": 10001
}
```

### Success Response

```json
{
  "assignmentId": 5001,
  "status": "ASSIGNED"
}
```

### Audit Event

```text
AUTO_ASSIGNMENT_TRIGGERED
```

---

## Bulk Assignment

### Endpoint

```http
POST /api/v1/assignments/bulk-auto-assign
```

### Description

Trigger assignment for all queued claims.

### Success Response

```json
{
  "queuedClaimsProcessed": 25,
  "assignmentsCreated": 23,
  "failedAssignments": 2
}
```

### Audit Event

```text
BULK_AUTO_ASSIGNMENT_TRIGGERED
```

---

# 8. Assignment Queue APIs

---

## Get Assignment Queue

### Endpoint

```http
GET /api/v1/assignment-queue
```

### Permissions

```text
ASSIGNMENT_QUEUE_VIEW
```

### Query Parameters

```text
page
size
priority
claimTypeId
branchId
```

### Success Response

```json
{
  "content": [
    {
      "queueId": 101,
      "claimId": 10001,
      "priority": "HIGH",
      "queuedAt": "2026-06-01T10:00:00Z"
    }
  ]
}
```

---

## Requeue Claim

### Endpoint

```http
POST /api/v1/assignment-queue/{queueId}/requeue
```

### Permissions

```text
ASSIGNMENT_QUEUE_MANAGE
```

### Success Response

```json
{
  "status": "QUEUED"
}
```

### Audit Event

```text
CLAIM_REQUEUED
```

---

## Remove From Queue

### Endpoint

```http
DELETE /api/v1/assignment-queue/{queueId}
```

### Success Response

```http
204 No Content
```

### Audit Event

```text
CLAIM_REMOVED_FROM_QUEUE
```

---

# 9. Assignment Acceptance APIs

---

## Accept Assignment

### Endpoint

```http
PATCH /api/v1/assignments/{assignmentId}/accept
```

### Permissions

```text
ASSIGNMENT_UPDATE
```

### Success Response

```json
{
  "status": "ACCEPTED"
}
```

### Audit Event

```text
ASSIGNMENT_ACCEPTED
```

---

## Reject Assignment

### Endpoint

```http
PATCH /api/v1/assignments/{assignmentId}/reject
```

### Request Body

```json
{
  "reason": "Conflict of interest"
}
```

### Success Response

```json
{
  "status": "REJECTED"
}
```

### Business Rules

* Claim automatically returns to queue.
* History record created.

### Audit Event

```text
ASSIGNMENT_REJECTED
```

---

# 10. Reassignment APIs

---

## Reassign Claim

### Endpoint

```http
POST /api/v1/assignments/{assignmentId}/reassign
```

### Permissions

```text
ASSIGNMENT_REASSIGN
```

### Request Body

```json
{
  "newInvestigatorId": 1002,
  "reason": "Workload balancing"
}
```

### Business Rules

* Existing assignment closed.
* New assignment created.
* History preserved.

### Success Response

```json
{
  "newAssignmentId": 5002
}
```

### Audit Event

```text
CLAIM_REASSIGNED
```

---

## Bulk Reassignment

### Endpoint

```http
POST /api/v1/assignments/bulk-reassign
```

### Request Body

```json
{
  "assignmentIds": [5001, 5002],
  "newInvestigatorId": 1003,
  "reason": "Regional restructuring"
}
```

### Success Response

```json
{
  "successful": 2,
  "failed": 0
}
```

### Audit Event

```text
BULK_REASSIGNMENT_COMPLETED
```

---

# 11. Assignment History APIs

---

## Get Assignment History

### Endpoint

```http
GET /api/v1/assignments/{assignmentId}/history
```

### Success Response

```json
[
  {
    "historyId": 1,
    "action": "ASSIGNED",
    "performedBy": "EMP0001",
    "timestamp": "2026-06-01T10:00:00Z"
  },
  {
    "historyId": 2,
    "action": "REASSIGNED",
    "performedBy": "EMP0002",
    "timestamp": "2026-06-02T09:00:00Z"
  }
]
```

---

# 12. Investigator Availability APIs

---

## Get Investigator Workload

### Endpoint

```http
GET /api/v1/assignments/workload/{investigatorId}
```

### Success Response

```json
{
  "investigatorId": 1001,
  "activeAssignments": 18,
  "completedAssignments": 245,
  "pendingAssignments": 4
}
```

---

## Get Available Investigators

### Endpoint

```http
GET /api/v1/assignments/available-investigators
```

### Query Parameters

```text
branchId
regionId
claimTypeId
```

### Success Response

```json
[
  {
    "userId": 1001,
    "name": "John Doe",
    "activeAssignments": 18,
    "capacityRemaining": 32
  }
]
```

---

# 13. Assignment Analytics APIs

---

## Get Assignment Statistics

### Endpoint

```http
GET /api/v1/assignments/statistics
```

### Success Response

```json
{
  "totalAssignments": 500,
  "activeAssignments": 120,
  "completedAssignments": 350,
  "reassignedClaims": 30
}
```

---

## Get Queue Statistics

### Endpoint

```http
GET /api/v1/assignment-queue/statistics
```

### Success Response

```json
{
  "queuedClaims": 15,
  "highPriorityClaims": 4,
  "averageQueueTimeMinutes": 22
}
```

---

# 14. Assignment Policy Integration APIs

---

## Get Effective Assignment Policy

### Endpoint

```http
GET /api/v1/assignments/policies/{claimTypeId}
```

### Success Response

```json
{
  "assignmentStrategy": "LEAST_WORKLOAD",
  "maxAssignmentsPerInvestigator": 50
}
```

---

# 15. Business Validation Rules

## Assignment Creation

* Claim must exist.
* Investigator must be active.
* Active assignment must not already exist.
* Claim must belong to tenant.

## Assignment Acceptance

* Only assigned investigator may accept.
* Assignment must be in ASSIGNED status.

## Assignment Rejection

* Reason mandatory.
* Claim returns to queue.

## Reassignment

* New investigator must be active.
* Reassignment reason required.
* History must be preserved.

## Queue Management

* Duplicate queue entries prohibited.
* Queue priority configurable through policy engine.

---

# 16. Error Codes

| Error Code                   | Description              |
| ---------------------------- | ------------------------ |
| ASSIGNMENT_NOT_FOUND         | Assignment missing       |
| CLAIM_NOT_FOUND              | Claim missing            |
| INVESTIGATOR_NOT_FOUND       | Investigator missing     |
| ASSIGNMENT_ALREADY_EXISTS    | Active assignment exists |
| INVALID_ASSIGNMENT_STATUS    | Invalid status           |
| INVESTIGATOR_INACTIVE        | Investigator inactive    |
| QUEUE_ENTRY_NOT_FOUND        | Queue entry missing      |
| REASSIGNMENT_NOT_ALLOWED     | Reassignment blocked     |
| ASSIGNMENT_CAPACITY_EXCEEDED | Investigator overloaded  |
| TENANT_MISMATCH              | Cross-tenant access      |

---

# 17. Audit Events

```text
ASSIGNMENT_CREATED
AUTO_ASSIGNMENT_TRIGGERED
BULK_AUTO_ASSIGNMENT_TRIGGERED

ASSIGNMENT_ACCEPTED
ASSIGNMENT_REJECTED

CLAIM_REASSIGNED
BULK_REASSIGNMENT_COMPLETED

CLAIM_REQUEUED
CLAIM_REMOVED_FROM_QUEUE
```

---

# 18. Integration Events

Produced Events:

```text
CLAIM_ASSIGNED
CLAIM_REASSIGNED
ASSIGNMENT_ACCEPTED
ASSIGNMENT_REJECTED
CLAIM_QUEUED
```

Consumed By:

```text
CLAIM MODULE
INVESTIGATION MODULE
NOTIFICATION MODULE
ANALYTICS MODULE
```

---

# 19. Performance Requirements

| API                | Target   |
| ------------------ | -------- |
| Create Assignment  | < 300 ms |
| Auto Assignment    | < 500 ms |
| Queue Lookup       | < 200 ms |
| Reassignment       | < 300 ms |
| Workload Lookup    | < 200 ms |
| Assignment History | < 300 ms |

---

# 20. Security Requirements

All APIs require:

```text
JWT Authentication
Tenant Validation
Permission Validation
Audit Logging
```

Additional Controls:

* Investigators may only access their own assignments.
* Managers may access assignments within their scope.
* Cross-tenant assignments prohibited.
* Assignment decisions fully auditable.

---

# 21. Assignment Engine Notes

V1 Assignment Engine supports:

```text
ROUND_ROBIN
LEAST_WORKLOAD
REGION_BASED
BRANCH_BASED
MANUAL
```

Policy selection is driven by:

```text
assignment_policy
```

Automatic assignment execution occurs through the Assignment Worker consuming records from:

```text
assignment_queue
```

using:

```text
FOR UPDATE SKIP LOCKED
```

to prevent duplicate assignment processing.

---

# Approval

This document defines the Assignment Management API contract for ClaimLens and serves as the implementation reference for claim assignment workflows, investigator allocation, queue management, reassignment operations, workload balancing, and assignment policy execution.
