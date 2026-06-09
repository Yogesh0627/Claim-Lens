# 08.1 Event Standards

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | Event Design          |
| Document Number | 08.1                  |
| Document Name   | Event Standards       |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

This document defines the event-driven architecture standards used throughout ClaimLens.

The objective is to establish a consistent and scalable event model that supports:

* Modular Monolith Architecture
* OCR Service Integration
* Analysis Service Integration
* Asynchronous Processing
* Workflow Orchestration
* Audit Logging
* Analytics Aggregation
* Future Kafka Adoption

These standards apply to all events generated within ClaimLens.

---

# 2. Event Architecture

## Architectural Principle

ClaimLens follows an:

```text
Event-Driven Architecture
```

where business operations produce events that may be consumed by one or more modules.

Example:

```text
Claim Created
      ↓
CLAIM_CREATED
      ↓
Analytics Module
Audit Module
Notification Module
```

---

# 3. Event Categories

ClaimLens uses three event categories.

---

## 3.1 Domain Events

Represent business facts that occurred within a domain.

Examples:

```text
CLAIM_CREATED
CLAIM_UPDATED
DOCUMENT_UPLOADED
INVESTIGATION_COMPLETED
```

Characteristics:

* Produced by business modules
* Immutable
* Represent completed business actions

---

## 3.2 Integration Events

Used for communication between modules and services.

Examples:

```text
DOCUMENT_UPLOADED
OCR_COMPLETED
ANALYSIS_COMPLETED
FRAUD_EVALUATION_QUEUED
```

Characteristics:

* Cross-module communication
* May trigger workflows
* Consumed asynchronously

---

## 3.3 System Events

Represent operational and infrastructure activities.

Examples:

```text
OCR_FAILED
ANALYSIS_FAILED
RECOVERY_WORKER_EXECUTED
NOTIFICATION_DELIVERY_FAILED
```

Characteristics:

* Monitoring focused
* Operational visibility
* Recovery workflows

---

# 4. Event Architecture Decision

## Selected Pattern

ClaimLens uses:

```text
OUTBOX PATTERN
```

for reliable event delivery.

---

## Why Outbox Pattern

Benefits:

* Prevents lost events
* Prevents dual-write problem
* Supports retries
* Supports future Kafka migration
* Provides auditability

---

## Event Flow

```text
Business Transaction
         ↓
Commit Database Transaction
         ↓
Insert Outbox Event
         ↓
Outbox Publisher Worker
         ↓
Publish Event
         ↓
Consumers Process Event
```

---

# 5. Outbox Event Table

All events are first stored in:

```text
audit.outbox_event
```

Example Structure:

```text
event_id
event_type
aggregate_type
aggregate_id
payload
status
retry_count
created_at
published_at
```

---

# 6. Event Lifecycle

## Event States

```text
PENDING
PUBLISHED
FAILED
DEAD_LETTER
```

---

## Lifecycle Flow

```text
PENDING
    ↓
PUBLISHED

OR

PENDING
    ↓
FAILED
    ↓
RETRY
    ↓
PUBLISHED

OR

FAILED
    ↓
DEAD_LETTER
```

---

# 7. Event Naming Convention

Format:

```text
ENTITY_ACTION
```

Examples:

```text
CLAIM_CREATED
CLAIM_UPDATED
CLAIM_STATUS_CHANGED

DOCUMENT_UPLOADED
DOCUMENT_VERSION_CREATED

ASSIGNMENT_CREATED

INVESTIGATION_COMPLETED

FRAUD_ALERT_CREATED
```

---

## Naming Rules

Events must:

* Use uppercase
* Use underscores
* Represent completed actions
* Use past tense

Good:

```text
CLAIM_CREATED
```

Bad:

```text
CREATE_CLAIM
```

---

# 8. Event Envelope Standard

Every event must follow the same envelope structure.

---

## Standard Event Envelope

```json
{
  "eventId": "uuid",
  "eventType": "CLAIM_CREATED",
  "eventVersion": 1,
  "tenantId": 1,
  "aggregateType": "CLAIM",
  "aggregateId": 10001,
  "occurredAt": "2026-06-01T10:00:00Z",
  "correlationId": "uuid",
  "causationId": "uuid",
  "producer": "claim-module",
  "payload": {}
}
```

---

# 9. Event Metadata

## eventId

Unique event identifier.

Purpose:

```text
Deduplication
Idempotency
Tracing
```

---

## correlationId

Used to track an entire workflow.

Example:

```text
Claim Submission
       ↓
Document Upload
       ↓
OCR
       ↓
Fraud Evaluation
```

All share the same:

```text
correlationId
```

---

## causationId

Identifies the event that triggered another event.

Example:

```text
DOCUMENT_UPLOADED
        ↓
OCR_JOB_CREATED
```

OCR_JOB_CREATED stores:

```text
causationId = DOCUMENT_UPLOADED.eventId
```

---

# 10. Event Versioning

## Version Strategy

Every event includes:

```text
eventVersion
```

---

## Initial Version

```json
{
  "eventVersion": 1
}
```

---

## Future Changes

Breaking changes require:

```text
Version Increment
```

Example:

```text
CLAIM_CREATED V1
CLAIM_CREATED V2
```

Consumers must support their expected version.

---

# 11. Idempotency Requirements

All consumers must be idempotent.

---

## Why

Events may be delivered more than once.

Example:

```text
DOCUMENT_UPLOADED
```

published twice.

Consumer must process once.

---

## Strategy

Store:

```text
processed_event
```

records.

Example:

```text
event_id
consumer_name
processed_at
```

---

# 12. Retry Strategy

Transient failures are retried.

---

## Retry Schedule

```text
Attempt 1 : Immediate
Attempt 2 : 1 Minute
Attempt 3 : 5 Minutes
Attempt 4 : 15 Minutes
Attempt 5 : 30 Minutes
```

Maximum:

```text
5 Attempts
```

---

# 13. Dead Letter Strategy

After retry limit exceeded:

```text
DEAD_LETTER
```

status assigned.

---

## Dead Letter Queue Purpose

Allows:

* Investigation
* Replay
* Operational monitoring

---

# 14. Consumer Rules

Consumers must:

* Be idempotent
* Handle retries
* Validate event version
* Validate tenant ownership
* Log failures
* Emit audit events

Consumers must not:

* Mutate original events
* Assume delivery order
* Depend on synchronous execution

---

# 15. Producer Rules

Producers must:

* Publish only after transaction commit
* Include full event metadata
* Use event envelope standard
* Use correlation IDs

Producers must not:

* Publish before commit
* Publish partial payloads
* Publish invalid tenant data

---

# 16. Event Ordering

ClaimLens does not guarantee global event ordering.

Ordering guaranteed only within:

```text
aggregate_id
```

Example:

```text
Claim 10001
```

Events:

```text
CLAIM_CREATED
CLAIM_UPDATED
CLAIM_STATUS_CHANGED
```

must remain ordered.

---

# 17. Event Payload Design Rules

Payloads must:

* Be self-contained
* Contain required identifiers
* Avoid database lookups where possible
* Avoid sensitive data

Good:

```json
{
  "claimId": 10001,
  "claimNumber": "CLM-2026-000001"
}
```

Bad:

```json
{
  "claimName": "Full customer details..."
}
```

---

# 18. Event Security

Events must never contain:

```text
Passwords
JWT Tokens
API Keys
Secrets
Raw PII
Payment Information
```

Sensitive information must be referenced by identifier only.

---

# 19. Event Monitoring

Metrics tracked:

```text
Events Published
Events Consumed
Events Failed
Events Retried
Dead Letter Events
```

Prometheus Metrics:

```text
event_publish_total
event_consume_total
event_failure_total
event_retry_total
dead_letter_total
```

---

# 20. Event Traceability

Every workflow must support tracing.

Example:

```text
CLAIM_CREATED
      ↓
DOCUMENT_UPLOADED
      ↓
OCR_COMPLETED
      ↓
ANALYSIS_COMPLETED
      ↓
FRAUD_EVALUATED
```

Traceable through:

```text
correlationId
```

---

# 21. Event Catalog Structure

Each event document must define:

```text
Event Name

Category

Producer

Consumers

Trigger

Payload

Business Rules

Failure Handling
```

This structure must be followed for all Event Design documents.

---

# 22. Future Kafka Migration

Current V1:

```text
Outbox Table
     ↓
Publisher Worker
     ↓
Consumers
```

Future V2:

```text
Outbox Table
     ↓
Kafka
     ↓
Consumers
```

No business event contract changes required.

---

# Approval

This document defines the Event Architecture Standards for ClaimLens and serves as the foundation for all domain events, integration events, system events, asynchronous workflows, outbox processing, event versioning, retry handling, and future event-driven scalability.
