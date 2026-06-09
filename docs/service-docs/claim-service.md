# 09.2 Claim Service Design

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | Service Design        |
| Document Number | 09.2                  |
| Document Name   | Claim Service Design  |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

The Claim Module is the core business module of ClaimLens.

Responsibilities:

* Claim Creation
* Claim Management
* Claim Status Management
* Claim Comments
* Claim Tags
* Additional Information Requests
* Claim Search
* Claim History Tracking

The Claim Module acts as the primary aggregate root for the insurance claim lifecycle.

---

# 2. Module Dependencies

## Incoming Dependencies

Consumed Events:

```text
INVESTIGATION_COMPLETED

FRAUD_REVIEW_COMPLETED

FRAUD_ALERT_CLOSED
```

---

## Outgoing Dependencies

Published Events:

```text
CLAIM_CREATED

CLAIM_UPDATED

CLAIM_STATUS_CHANGED

CLAIM_COMMENT_ADDED

CLAIM_TAG_ASSIGNED

CLAIM_TAG_REMOVED

INFORMATION_REQUEST_CREATED

INFORMATION_REQUEST_RESPONDED

CLAIM_CLOSED
```

---

# 3. Package Structure

```text
claim

├── controller
│   └── ClaimController

├── service
│   ├── ClaimService
│   └── ClaimServiceImpl

├── repository
│   ├── ClaimRepository
│   ├── ClaimHistoryRepository
│   ├── ClaimCommentRepository
│   └── ClaimTagMappingRepository

├── entity
│   ├── Claim
│   ├── ClaimHistory
│   ├── ClaimComment
│   ├── ClaimTag
│   └── ClaimTagMapping

├── dto
│   ├── request
│   └── response

├── mapper

├── validator

├── event

├── exception

├── specification
```

---

# 4. Domain Entities

Primary Aggregate:

```text
Claim
```

Supporting Entities:

```text
ClaimHistory

ClaimComment

ClaimTag

ClaimTagMapping
```

---

# 5. Controller Layer

## ClaimController

Base Path:

```http
/api/v1/claims
```

---

## Endpoints

```java
POST   /claims

GET    /claims

GET    /claims/{claimId}

PUT    /claims/{claimId}

PATCH  /claims/{claimId}/status

POST   /claims/{claimId}/comments

POST   /claims/{claimId}/tags

DELETE /claims/{claimId}/tags/{tagId}

POST   /claims/{claimId}/information-requests

POST   /claims/{claimId}/information-responses

GET    /claims/{claimId}/history
```

---

# 6. Service Layer

## ClaimService

```java
public interface ClaimService
```

---

## Methods

### Create Claim

```java
ClaimResponse createClaim(
    CreateClaimRequest request
);
```

---

### Get Claim

```java
ClaimResponse getClaim(
    Long claimId
);
```

---

### Search Claims

```java
Page<ClaimResponse> searchClaims(
    ClaimSearchRequest request
);
```

---

### Update Claim

```java
ClaimResponse updateClaim(
    Long claimId,
    UpdateClaimRequest request
);
```

---

### Update Status

```java
void updateStatus(
    Long claimId,
    UpdateClaimStatusRequest request
);
```

---

### Add Comment

```java
void addComment(
    Long claimId,
    AddCommentRequest request
);
```

---

### Assign Tag

```java
void assignTag(
    Long claimId,
    AssignTagRequest request
);
```

---

### Remove Tag

```java
void removeTag(
    Long claimId,
    Long tagId
);
```

---

### Create Information Request

```java
void createInformationRequest(
    Long claimId,
    CreateInformationRequest request
);
```

---

### Submit Information Response

```java
void submitInformationResponse(
    Long claimId,
    SubmitInformationResponseRequest request
);
```

---

# 7. DTO Design

## CreateClaimRequest

```java
tenantId

customerId

claimTypeId

policyNumber

incidentDate

claimAmount

description
```

---

## ClaimResponse

```java
id

claimNumber

status

claimAmount

customerId

claimTypeId

createdAt
```

---

## ClaimSearchRequest

```java
claimNumber

customerNumber

status

claimType

dateFrom

dateTo

page

size
```

---

# 8. Repository Layer

## ClaimRepository

```java
extends JpaRepository<Claim, Long>
```

---

## Methods

```java
Optional<Claim>
findByClaimNumber(
    String claimNumber
);
```

---

```java
boolean existsByClaimNumber(
    String claimNumber
);
```

---

```java
Page<Claim>
findAll(
    Specification<Claim> specification,
    Pageable pageable
);
```

---

# 9. Specifications

## ClaimSpecification

Supported Filters:

```text
claimNumber

customerNumber

status

claimType

assignedUser

fraudScoreRange

createdDateRange
```

---

# 10. Validation Layer

## ClaimValidator

Responsibilities:

```text
Claim Creation Validation

Status Transition Validation

Policy Validation

Tenant Validation
```

---

## Example

Allowed:

```text
SUBMITTED
     ↓
UNDER_REVIEW
```

---

Forbidden:

```text
CLOSED
     ↓
UNDER_REVIEW
```

---

# 11. Mapper Layer

## ClaimMapper

Responsibilities:

```text
Request → Entity

Entity → Response
```

Recommended:

```text
MapStruct
```

---

# 12. Event Publishing

## ClaimEventPublisher

Responsibilities:

```text
Create Outbox Events
```

---

## Events

```text
CLAIM_CREATED

CLAIM_UPDATED

CLAIM_STATUS_CHANGED

CLAIM_COMMENT_ADDED

CLAIM_TAG_ASSIGNED

CLAIM_TAG_REMOVED

INFORMATION_REQUEST_CREATED

INFORMATION_REQUEST_RESPONDED

CLAIM_CLOSED
```

---

# 13. Transaction Boundaries

## Create Claim

```java
@Transactional
```

Workflow:

```text
Validate Request
      ↓
Create Claim
      ↓
Persist Claim
      ↓
Create History Record
      ↓
Save Outbox Event
      ↓
Commit
```

---

## Update Status

```java
@Transactional
```

Workflow:

```text
Validate Transition
      ↓
Update Status
      ↓
Create History
      ↓
Save Outbox Event
      ↓
Commit
```

---

# 14. Claim History Strategy

Every change generates:

```text
ClaimHistory Record
```

Tracked:

```text
Status Changes

Assignments

Comments

Information Requests

Claim Closure
```

---

# 15. Exception Handling

## Exceptions

```java
ClaimNotFoundException

InvalidClaimStatusException

DuplicateClaimException

PolicyValidationException
```

---

# 16. Security Rules

Required Roles:

```text
CLAIM_CREATE

CLAIM_VIEW

CLAIM_UPDATE

CLAIM_CLOSE
```

---

Tenant Isolation:

```text
Every Query Must Filter
By tenant_id
```

---

# 17. Audit Integration

Every write operation creates:

```text
AUDIT_EVENT_CREATED
```

Examples:

```text
Claim Created

Claim Updated

Status Changed

Claim Closed
```

---

# 18. Performance Considerations

Indexes:

```text
claim_number

customer_id

status

claim_type_id

tenant_id
```

---

Search APIs:

```text
Pagination Mandatory
```

---

# 19. Dependency Diagram

```text
ClaimController
        ↓
ClaimService
        ↓
ClaimValidator
        ↓
ClaimRepository
        ↓
PostgreSQL

ClaimService
        ↓
ClaimEventPublisher
        ↓
Outbox Table
```

---

# 20. Unit Testing Requirements

Coverage Target:

```text
90%+
```

Required Tests:

```text
Create Claim

Update Claim

Status Transition

Comment Creation

Tag Assignment

Information Request

Claim Closure
```

---

# 21. Future Enhancements

V2 Reserved:

```text
Claim Merge

Claim Split

Bulk Claim Import

AI Claim Summaries

Duplicate Claim Detection
```

---

# Approval

This document defines the Claim Module implementation blueprint and serves as the reference for controller design, service implementation, repository access, validation, event publishing, transaction management, audit integration, and future scalability.
