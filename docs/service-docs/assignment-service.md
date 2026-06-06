# 09.4 Assignment Service Design

## Document Information

| Field           | Value                     |
| --------------- | ------------------------- |
| Project         | ClaimLens                 |
| Company         | RiskLens Technologies     |
| Version         | V1                        |
| Document Type   | Service Design            |
| Document Number | 09.4                      |
| Document Name   | Assignment Service Design |
| Status          | Approved                  |
| Last Updated    | June 2026                 |

---

# 1. Overview

The Assignment Module is responsible for assigning fraud investigations to investigators.

Responsibilities:

* Assignment Queue Management
* Auto Assignment
* Manual Assignment
* Reassignment
* Workload Distribution
* SLA Monitoring
* Escalation Handling

The Assignment Module acts as the bridge between Fraud Detection and Investigation.

---

# 2. Module Dependencies

## Consumed Events

```text
FRAUD_ALERT_CREATED

CLAIM_PROCESSING_COMPLETED

ASSIGNMENT_REJECTED

INVESTIGATION_COMPLETED
```

---

## Published Events

```text
ASSIGNMENT_QUEUE_ENTERED

ASSIGNMENT_CREATED

ASSIGNMENT_ACCEPTED

ASSIGNMENT_REJECTED

CLAIM_REASSIGNED

ASSIGNMENT_ESCALATED

ASSIGNMENT_COMPLETED

ASSIGNMENT_QUEUE_EXITED
```

---

# 3. Package Structure

```text
assignment

├── controller
│   └── AssignmentController

├── service
│   ├── AssignmentService
│   ├── AssignmentEngine
│   ├── WorkloadService
│   ├── EscalationService
│   └── AssignmentServiceImpl

├── repository
│   ├── AssignmentRepository
│   ├── AssignmentQueueRepository
│   └── AssignmentHistoryRepository

├── entity
│   ├── Assignment
│   ├── AssignmentQueue
│   └── AssignmentHistory

├── dto

├── mapper

├── validator

├── event

├── exception

├── scheduler
```

---

# 4. Domain Entities

Primary Aggregate:

```text
Assignment
```

Supporting Entities:

```text
AssignmentQueue

AssignmentHistory
```

---

# 5. Controller Layer

## AssignmentController

Base Path

```http
/api/v1/assignments
```

---

## Endpoints

```http
GET    /assignments

GET    /assignments/{assignmentId}

POST   /assignments/manual

POST   /assignments/{assignmentId}/accept

POST   /assignments/{assignmentId}/reject

POST   /assignments/{assignmentId}/reassign

POST   /assignments/{assignmentId}/complete

GET    /assignments/queue

GET    /assignments/investigators/{userId}
```

---

# 6. Service Layer

## AssignmentService

```java
public interface AssignmentService
```

---

## Methods

### Create Assignment

```java
AssignmentResponse createAssignment(
    CreateAssignmentRequest request
);
```

---

### Manual Assignment

```java
AssignmentResponse manualAssign(
    ManualAssignmentRequest request
);
```

---

### Accept Assignment

```java
void acceptAssignment(
    Long assignmentId
);
```

---

### Reject Assignment

```java
void rejectAssignment(
    Long assignmentId,
    RejectAssignmentRequest request
);
```

---

### Reassign

```java
void reassignAssignment(
    Long assignmentId,
    ReassignAssignmentRequest request
);
```

---

### Complete Assignment

```java
void completeAssignment(
    Long assignmentId
);
```

---

# 7. Assignment Engine

## Purpose

Responsible for automatic investigator allocation.

---

## Assignment Criteria

```text
Region

Branch

Workload

Claim Priority

Fraud Severity

Availability
```

---

## Algorithm V1

```text
Eligible Investigators
         ↓
Filter Active Users
         ↓
Filter Branch
         ↓
Filter Region
         ↓
Sort By Workload
         ↓
Assign Lowest Load Investigator
```

---

## Future Algorithms

```text
Skill Based Routing

ML Assignment

Fraud Specialist Assignment
```

---

# 8. Workload Service

Responsibilities:

```text
Current Active Assignments

Assignments Per Investigator

Workload Balancing

Capacity Monitoring
```

---

## Example Query

```java
Map<Long,Integer>
getInvestigatorWorkloads();
```

---

# 9. Assignment Queue Management

## Queue Entry

Source Event:

```text
FRAUD_ALERT_CREATED
```

---

## Queue Processing Flow

```text
Fraud Alert
      ↓
Assignment Queue
      ↓
Assignment Engine
      ↓
Assignment Created
```

---

## Queue Statuses

```text
PENDING

PROCESSING

ASSIGNED

FAILED
```

---

# 10. DTO Design

## CreateAssignmentRequest

```java
claimId

fraudAlertId

priority
```

---

## AssignmentResponse

```java
assignmentId

claimId

investigatorId

status

assignedAt
```

---

## ReassignAssignmentRequest

```java
newInvestigatorId

reason
```

---

# 11. Repository Layer

## AssignmentRepository

```java
extends JpaRepository<Assignment, Long>
```

---

## Methods

```java
List<Assignment>
findByInvestigatorIdAndStatus(
    Long investigatorId,
    AssignmentStatus status
);
```

---

```java
Optional<Assignment>
findByClaimId(Long claimId);
```

---

## AssignmentQueueRepository

```java
extends JpaRepository<
    AssignmentQueue,
    Long
>
```

---

# 12. Validation Layer

## AssignmentValidator

Responsibilities:

```text
Investigator Validation

Workload Validation

Status Transition Validation

Policy Validation
```

---

## Examples

Allowed:

```text
PENDING
    ↓
ACCEPTED
```

---

Forbidden:

```text
COMPLETED
    ↓
ACCEPTED
```

---

# 13. Assignment History Strategy

Every action generates:

```text
AssignmentHistory
```

Tracked Actions:

```text
Assigned

Accepted

Rejected

Reassigned

Escalated

Completed
```

---

# 14. Event Publishing

## AssignmentEventPublisher

Events:

```text
ASSIGNMENT_QUEUE_ENTERED

ASSIGNMENT_CREATED

ASSIGNMENT_ACCEPTED

ASSIGNMENT_REJECTED

CLAIM_REASSIGNED

ASSIGNMENT_ESCALATED

ASSIGNMENT_COMPLETED

ASSIGNMENT_QUEUE_EXITED
```

---

## Outbox Pattern

All events saved into:

```text
audit.outbox_event
```

---

# 15. SLA Escalation Service

## Responsibilities

```text
SLA Monitoring

Escalation Detection

Escalation Event Generation
```

---

## Example Rule

```text
Assignment Not Accepted
Within 24 Hours
```

Generate:

```text
ASSIGNMENT_ESCALATED
```

---

# 16. Scheduled Jobs

## Queue Worker

Runs:

```text
Every 1 Minute
```

Purpose:

```text
Process Pending Queue Items
```

---

## Escalation Worker

Runs:

```text
Every 15 Minutes
```

Purpose:

```text
Detect SLA Violations
```

---

# 17. Transaction Boundaries

## Auto Assignment

```java
@Transactional
```

Workflow:

```text
Select Investigator
        ↓
Create Assignment
        ↓
Update Queue
        ↓
Create History
        ↓
Save Outbox Event
        ↓
Commit
```

---

## Reassignment

```java
@Transactional
```

Workflow:

```text
Validate Investigator
       ↓
Update Assignment
       ↓
Create History
       ↓
Publish Event
       ↓
Commit
```

---

# 18. Exception Handling

Exceptions:

```java
AssignmentNotFoundException

InvestigatorNotFoundException

AssignmentPolicyViolationException

WorkloadLimitExceededException
```

---

# 19. Security Rules

Required Permissions:

```text
ASSIGNMENT_VIEW

ASSIGNMENT_CREATE

ASSIGNMENT_REASSIGN

ASSIGNMENT_COMPLETE
```

---

Tenant Isolation:

```text
All Queries Filtered
By tenant_id
```

---

# 20. Audit Integration

Tracked:

```text
Assignment Created

Assignment Accepted

Assignment Rejected

Assignment Reassigned

Assignment Escalated

Assignment Completed
```

---

Every write action generates:

```text
AUDIT_EVENT_CREATED
```

---

# 21. Performance Considerations

Indexes:

```text
claim_id

investigator_id

status

tenant_id

priority
```

---

Queue Queries:

```text
FOR UPDATE SKIP LOCKED
```

Used for concurrent worker processing.

---

# 22. Dependency Diagram

```text
AssignmentController
         ↓
AssignmentService
         ↓
AssignmentEngine
         ↓
WorkloadService
         ↓
Repositories
         ↓
PostgreSQL

AssignmentService
         ↓
AssignmentEventPublisher
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
Auto Assignment

Manual Assignment

Reassignment

Assignment Acceptance

Assignment Rejection

Workload Distribution

Escalation Detection

Queue Processing
```

---

# 24. Future Enhancements

V2 Reserved:

```text
Skill Based Assignment

Fraud Specialist Routing

AI Assignment Recommendations

Cross Region Assignment
```

---

# Approval

This document defines the Assignment Module implementation blueprint and serves as the reference for assignment workflows, queue management, workload balancing, SLA escalation, event publishing, transaction management, audit integration, and future scalability.
