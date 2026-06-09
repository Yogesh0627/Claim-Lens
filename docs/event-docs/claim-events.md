# 08.2 Claim Events

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | Event Design          |
| Document Number | 08.2                  |
| Document Name   | Claim Events          |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

This document defines all Claim Domain events produced by the Claim Module.

These events represent business facts that occur during the lifecycle of an insurance claim.

Claim events are consumed by:

* Assignment Module
* Investigation Module
* Fraud Module
* Notification Module
* Analytics Module
* Audit Module

All events follow the standard envelope defined in:

```text
08.1-event-standards.md
```

---

# 2. Claim Event Catalog

| Event                         | Category             |
| ----------------------------- | -------------------- |
| CLAIM_CREATED                 | Domain + Integration |
| CLAIM_UPDATED                 | Domain               |
| CLAIM_STATUS_CHANGED          | Domain + Integration |
| CLAIM_COMMENT_ADDED           | Domain               |
| CLAIM_TAG_ASSIGNED            | Domain               |
| CLAIM_TAG_REMOVED             | Domain               |
| INFORMATION_REQUEST_CREATED   | Integration          |
| INFORMATION_REQUEST_RESPONDED | Integration          |
| CLAIM_READY_FOR_ASSIGNMENT    | Integration          |
| CLAIM_CLOSED                  | Domain + Integration |

---

# 3. CLAIM_CREATED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

A new insurance claim is successfully submitted.

---

## Producer

```text
Claim Module
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
  "claimId": 10001,
  "claimNumber": "CLM-2026-000001",
  "claimTypeId": 1,
  "customerId": 501,
  "policyNumber": "POL-123456",
  "claimAmount": 250000,
  "status": "SUBMITTED"
}
```

---

## Business Rules

* Claim must be committed successfully.
* Claim number must be generated.
* Event published only after transaction commit.

---

## Failure Handling

```text
Outbox Retry
Dead Letter Support
```

---

# 4. CLAIM_UPDATED

## Category

```text
Domain Event
```

---

## Trigger

Claim details modified.

Examples:

```text
Claim Amount Updated
Customer Details Corrected
Policy Reference Updated
```

---

## Producer

```text
Claim Module
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
  "claimId": 10001,
  "updatedFields": [
    "claimAmount"
  ]
}
```

---

# 5. CLAIM_STATUS_CHANGED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Claim status changes.

---

## Valid Statuses

```text
SUBMITTED
UNDER_REVIEW
WAITING_FOR_INFORMATION
INVESTIGATION_REQUIRED
APPROVED
REJECTED
CLOSED
```

---

## Producer

```text
Claim Module
```

---

## Consumers

```text
Assignment Module
Investigation Module
Notification Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "claimId": 10001,
  "claimNumber": "CLM-2026-000001",
  "oldStatus": "SUBMITTED",
  "newStatus": "UNDER_REVIEW",
  "reason": "Initial review completed"
}
```

---

## Business Rules

Status transition must be valid according to claim workflow.

---

# 6. CLAIM_COMMENT_ADDED

## Category

```text
Domain Event
```

---

## Trigger

Comment added to claim.

---

## Producer

```text
Claim Module
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
  "claimId": 10001,
  "commentId": 3001,
  "commentedBy": 101
}
```

---

# 7. CLAIM_TAG_ASSIGNED

## Category

```text
Domain Event
```

---

## Trigger

Tag assigned to claim.

---

## Producer

```text
Claim Module
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
  "tagId": 10,
  "tagName": "HIGH_PRIORITY"
}
```

---

# 8. CLAIM_TAG_REMOVED

## Category

```text
Domain Event
```

---

## Trigger

Tag removed from claim.

---

## Producer

```text
Claim Module
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
  "tagId": 10,
  "tagName": "HIGH_PRIORITY"
}
```

---

# 9. INFORMATION_REQUEST_CREATED

## Category

```text
Integration Event
```

---

## Trigger

Additional information requested from customer.

---

## Producer

```text
Claim Module
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
  "requestId": 5001,
  "requestReason": "Vehicle RC copy missing",
  "responseDueDate": "2026-06-15"
}
```

---

## Business Rules

* Request must be persisted first.
* Due date mandatory.

---

# 10. INFORMATION_REQUEST_RESPONDED

## Category

```text
Integration Event
```

---

## Trigger

Customer submits requested information.

---

## Producer

```text
Claim Module
```

---

## Consumers

```text
Processing Module
Notification Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "claimId": 10001,
  "requestId": 5001,
  "responseSubmittedAt": "2026-06-10T10:00:00Z"
}
```

---

## Business Rules

Response must be linked to existing request.

---

# 11. CLAIM_READY_FOR_ASSIGNMENT

## Category

```text
Integration Event
```

---

## Trigger

Claim becomes eligible for investigator assignment.

---

## Producer

```text
Claim Module
```

---

## Consumers

```text
Assignment Module
Analytics Module
Audit Module
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

Generated when:

```text
All mandatory documents uploaded
Initial validation passed
```

---

# 12. CLAIM_CLOSED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Claim lifecycle completed.

---

## Producer

```text
Claim Module
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
  "claimNumber": "CLM-2026-000001",
  "finalStatus": "APPROVED",
  "closedAt": "2026-06-20T16:00:00Z"
}
```

---

## Business Rules

Only terminal statuses allowed:

```text
APPROVED
REJECTED
```

---

# 13. Event Relationships

## Claim Creation Flow

```text
CLAIM_CREATED
      ↓
DOCUMENT_UPLOADED
      ↓
CLAIM_READY_FOR_ASSIGNMENT
      ↓
ASSIGNMENT_CREATED
```

---

## Information Request Flow

```text
INFORMATION_REQUEST_CREATED
      ↓
Customer Uploads Documents
      ↓
INFORMATION_REQUEST_RESPONDED
      ↓
DOCUMENT_UPLOADED
```

---

## Claim Closure Flow

```text
INVESTIGATION_COMPLETED
      ↓
CLAIM_STATUS_CHANGED
      ↓
CLAIM_CLOSED
```

---

# 14. Correlation Strategy

All claim-related events must use:

```text
correlationId = claimId workflow identifier
```

Example:

```text
CLAIM_CREATED
DOCUMENT_UPLOADED
OCR_COMPLETED
ANALYSIS_COMPLETED
FRAUD_EVALUATED
CLAIM_CLOSED
```

All belong to the same claim workflow chain.

---

# 15. Idempotency Requirements

Consumers must process:

```text
CLAIM_CREATED
CLAIM_STATUS_CHANGED
CLAIM_CLOSED
```

idempotently.

Duplicate deliveries must not create duplicate records.

---

# 16. Monitoring Metrics

Prometheus Metrics:

```text
claim_events_published_total

claim_created_total
claim_updated_total
claim_closed_total

claim_event_failures_total
```

---

# 17. Security Requirements

Event payloads must never contain:

```text
Passwords
Authentication Tokens
Bank Details
Sensitive Customer PII
```

Only identifiers may be included.

---

# Approval

This document defines the Claim Domain Event Catalog for ClaimLens and serves as the implementation reference for claim lifecycle events, workflow orchestration, assignment initiation, customer information requests, analytics aggregation, notification generation, and audit tracking.
