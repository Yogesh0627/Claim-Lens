> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; the authoritative behaviour is the service code. Where this diverges, the code wins ([`../architecture.md`](../architecture.md), [`../domain-model.md`](../domain-model.md), [`../audit-report.md`](../audit-report.md)); deltas flagged inline as **As-built** notes.

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

**As-built (2026-07-29):** the module ships as a **three-step intake** — `createDraft` → upload documents → `submit` — with a much smaller surface than described here. Comments and tags were **not built** (no `ClaimComment`/`ClaimTag`/`ClaimTagMapping`); the only supporting entity is `ClaimStatusHistory`. The unique claim number is generated at **DRAFT** (`generateClaimNumber()`), and `submit` moves the claim to **AWAITING_ANALYSIS** (background OCR + analysis, then the fraud gate). The status enum has **11 values** (`DRAFT, SUBMITTED, AWAITING_ANALYSIS, AWAITING_ASSIGNMENT, AWAITING_ACCEPTANCE, UNDER_INVESTIGATION, WAITING_FOR_CUSTOMER, APPROVED, REJECTED, CLOSED, REOPENED`) — there is **no `CANCELLED`** and no `UNDER_REVIEW`; `REOPENED` exists but its transition is not wired.

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

**As-built (2026-07-29):** the real surface is workflow-verb based, each guarded by `@PreAuthorize` on the impl method (permission in parens):
`POST /claims` (createDraft, `CLAIM_WRITE`) · `POST /claims/{id}/submit` (`CLAIM_SUBMIT`) · `POST /claims/{id}/assign` · `POST /claims/{id}/auto-assign` · `POST /claims/{id}/reassign` (all `CLAIM_ASSIGN`) · `POST /claims/{id}/decision` (`CLAIM_DECIDE`) · `POST /claims/{id}/request-information` (`CLAIM_INVESTIGATE`) · `GET /claims/{id}` and `GET /claims?page&size`→`PagedResponse` (`CLAIM_READ`). There is **no** `PUT /claims/{id}`, no `PATCH …/status`, and no comments/tags/history endpoints. Documents live under `/claims/{id}/documents`; investigation notes under `/claims/{id}/investigation-notes`; the status timeline is served to the portal as `/portal/claims/{id}/timeline`.

---

# 6. Service Layer

## ClaimService

```java
public interface ClaimService
```

**As-built (2026-07-29):** the built methods are `createDraft`, `submit`, `assign`/`autoAssign`/`reassign`, `decide`, `requestInformation`, `getClaim`, `getClaims` (paginated). `submit` runs `ClaimSubmissionValidator` (hard failures throw and reject; soft signals return as warnings — never a rejection). `assign`/`autoAssign` require the claim at **AWAITING_ASSIGNMENT**, create a `ClaimAssignment` and move the claim straight to **UNDER_INVESTIGATION** — there is **no separate acceptance step** wired (despite the `AWAITING_ACCEPTANCE` enum value). `reassign` only works while UNDER_INVESTIGATION: it retires live assignments (`ASSIGNED`→`REASSIGNED`) and creates a new one. `decide` (APPROVE/REJECT) sets `fraud_confirmed` on the claim (false on approve; on reject only if the investigator flags it). `requestInformation` moves UNDER_INVESTIGATION → WAITING_FOR_CUSTOMER; a customer document upload while waiting flips it back to UNDER_INVESTIGATION and re-runs processing.

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

**As-built (2026-07-29):** implemented as `ClaimSubmissionValidator`, run at **submit**. HARD checks (throw → reject): incident date not in the future, `POLICY_NOT_ACTIVE_ON_LOSS_DATE`, `CLAIMANT_NOT_POLICYHOLDER`, `VEHICLE_NOT_COVERED_BY_POLICY`, and **`DUPLICATE_CLAIM_EXISTS`** (another non-terminal claim for the same policy + normalized vehicle reg + incident date). SOFT signal (never rejects, returned as a warning): `POTENTIAL_OVER_LIMIT_CLAIM` (amount over sum insured). Tenant validation is **not** in this validator — it is automatic via Hibernate `@TenantId`.

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

**As-built (2026-07-29):** the real permission codes are `CLAIM_READ`, `CLAIM_WRITE`, `CLAIM_SUBMIT`, `CLAIM_ASSIGN`, `CLAIM_DECIDE`, `CLAIM_INVESTIGATE`, enforced via `@PreAuthorize("hasAuthority('…')")` on the impl methods (service layer, not the controller).

---

Tenant Isolation:

```text
Every Query Must Filter
By tenant_id
```

**As-built (2026-07-29):** not hand-filtered — isolation is automatic via Hibernate `@TenantId`; a cross-tenant claim id returns **404**.

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

**As-built (2026-07-29):** **Duplicate Claim Detection shipped in V1**, not V2 — it is a hard submit-time check (`DUPLICATE_CLAIM_EXISTS`, see §10). Also note the design's event/outbox integration (§12, §17) is not built: notifications and audit are direct synchronous in-transaction calls (see `service-design.md` §11).

---

# Approval

This document defines the Claim Module implementation blueprint and serves as the reference for controller design, service implementation, repository access, validation, event publishing, transaction management, audit integration, and future scalability.
