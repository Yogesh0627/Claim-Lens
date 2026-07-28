> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; the authoritative behaviour is the code. Where this diverges, the code wins ([`../architecture.md`](../architecture.md), [`../domain-model.md`](../domain-model.md)); deltas flagged inline as **As-built** notes.

# 08.4 Assignment Events

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | Event Design          |
| Document Number | 08.4                  |
| Document Name   | Assignment Events     |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

This document defines all events produced by the Assignment Module.

The Assignment Module is responsible for:

* Assignment Queue Management
* Investigator Assignment
* Workload Distribution
* Assignment Escalation
* Reassignment
* Assignment Completion

Assignment events bridge:

```text
Claim Processing
        ↓
Assignment
        ↓
Investigation
```

> **As-built (2026-07-29):** No assignment events are emitted, and there is **no assignment-queue entity** (`AssignmentQueue` was dropped) — so `ASSIGNMENT_QUEUE_ENTERED/EXITED`, `ASSIGNMENT_ACCEPTED/REJECTED`, `ASSIGNMENT_ESCALATED` and SLA handling do not exist. What exists: after processing a claim sits at `AWAITING_ASSIGNMENT`; a `CLAIM_ASSIGN`-permitted user calls `assign` (specific investigator) or `autoAssign` (`AssignmentEngine` picks the least-loaded eligible investigator in the tenant), creating a `claim_assignment` row and moving the claim to `UNDER_INVESTIGATION`. `reassign` retires the live assignment(s) and creates a new one. Each of these writes an in-app notification of type `CLAIM_ASSIGNED` synchronously (there is **no distinct `CLAIM_REASSIGNED` event** — reassignment reuses the `CLAIM_ASSIGNED` notification/email), plus an audit-log entry (`CLAIM_ASSIGNED`/`CLAIM_REASSIGNED` actions).

---

# 2. Assignment Event Catalog

| Event                    | Category             |
| ------------------------ | -------------------- |
| ASSIGNMENT_QUEUE_ENTERED | Integration          |
| ASSIGNMENT_CREATED       | Domain + Integration |
| ASSIGNMENT_ACCEPTED      | Domain               |
| ASSIGNMENT_REJECTED      | Domain               |
| CLAIM_REASSIGNED         | Integration          |
| ASSIGNMENT_ESCALATED     | Integration          |
| ASSIGNMENT_COMPLETED     | Domain + Integration |
| ASSIGNMENT_QUEUE_EXITED  | Integration          |

---

# 3. ASSIGNMENT_QUEUE_ENTERED

## Category

```text
Integration Event
```

---

## Trigger

Claim becomes eligible for assignment.

---

## Producer

```text
Assignment Module
```

---

## Consumers

```text
Analytics Module
Audit Module
```

---

## Source Event

```text
CLAIM_PROCESSING_COMPLETED
```

---

## Payload

```json
{
  "claimId": 10001,
  "claimNumber": "CLM-2026-000001",
  "priority": "HIGH",
  "claimAmount": 250000
}
```

---

## Business Rules

Claim must satisfy:

```text
All Required Documents Uploaded
OCR Complete
Analysis Complete
Fraud Evaluation Complete
```

---

# 4. ASSIGNMENT_CREATED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Investigator assigned to claim.

---

## Producer

```text
Assignment Module
```

---

## Consumers

```text
Investigation Module
Notification Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "assignmentId": 5001,
  "claimId": 10001,
  "investigatorId": 101,
  "assignmentType": "AUTO"
}
```

---

## Workflow

```text
ASSIGNMENT_CREATED
       ↓
INVESTIGATION_CREATED
```

---

# 5. ASSIGNMENT_ACCEPTED

## Category

```text
Domain Event
```

---

## Trigger

Investigator accepts assignment.

---

## Producer

```text
Assignment Module
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
  "assignmentId": 5001,
  "investigatorId": 101
}
```

---

# 6. ASSIGNMENT_REJECTED

## Category

```text
Domain Event
```

---

## Trigger

Investigator rejects assignment.

---

## Producer

```text
Assignment Module
```

---

## Consumers

```text
Assignment Module
Notification Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "assignmentId": 5001,
  "investigatorId": 101,
  "reason": "Workload Capacity Reached"
}
```

---

## Workflow

```text
ASSIGNMENT_REJECTED
         ↓
CLAIM_REASSIGNED
```

---

# 7. CLAIM_REASSIGNED

## Category

```text
Integration Event
```

---

## Trigger

Assignment moved to another investigator.

---

## Producer

```text
Assignment Module
```

---

## Consumers

```text
Notification Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "claimId": 10001,
  "oldInvestigatorId": 101,
  "newInvestigatorId": 102
}
```

---

# 8. ASSIGNMENT_ESCALATED

## Category

```text
Integration Event
```

---

## Trigger

Assignment SLA violated.

---

## Producer

```text
Assignment Module
```

---

## Consumers

```text
Notification Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "assignmentId": 5001,
  "slaHoursExceeded": 12
}
```

---

# 9. ASSIGNMENT_COMPLETED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Investigation assignment completed.

---

## Producer

```text
Assignment Module
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
  "assignmentId": 5001,
  "claimId": 10001,
  "completedAt": "2026-06-10T16:00:00Z"
}
```

---

# 10. ASSIGNMENT_QUEUE_EXITED

## Category

```text
Integration Event
```

---

## Trigger

Claim removed from assignment queue.

---

## Producer

```text
Assignment Module
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
  "assignmentId": 5001
}
```

---

# 11. Assignment Workflow

## Auto Assignment Flow

```text
CLAIM_PROCESSING_COMPLETED
          ↓
ASSIGNMENT_QUEUE_ENTERED
          ↓
ASSIGNMENT_CREATED
          ↓
ASSIGNMENT_QUEUE_EXITED
          ↓
INVESTIGATION_CREATED
```

---

## Reassignment Flow

```text
ASSIGNMENT_REJECTED
          ↓
CLAIM_REASSIGNED
          ↓
ASSIGNMENT_CREATED
```

---

## Escalation Flow

```text
ASSIGNMENT_CREATED
          ↓
SLA BREACH
          ↓
ASSIGNMENT_ESCALATED
```

---

# 12. Business Validation Rules

## Assignment Creation

* Investigator must be active.
* Workload limits respected.
* Assignment policy must exist.

## Reassignment

* Original assignment preserved.
* Reassignment reason mandatory.

## Escalation

* SLA policy required.
* Escalation tracked in audit log.

---

# 13. Monitoring Metrics

```text
assignment_created_total

assignment_completed_total

assignment_reassigned_total

assignment_escalated_total

assignment_queue_size
```

---

# 14. Security Requirements

Events must not contain:

```text
Customer PII
Investigator Credentials
Authentication Tokens
```

Only identifiers allowed.

---

# Approval

This document defines the Assignment Domain Event Catalog for ClaimLens and serves as the implementation reference for assignment workflows, workload distribution, queue processing, reassignment handling, SLA escalation, investigation initiation, analytics aggregation, and audit tracking.
