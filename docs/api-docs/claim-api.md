> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; where it diverges from the shipped code the authoritative sources win: [`../domain-model.md`](../domain-model.md), [`../architecture.md`](../architecture.md), [`../audit-report.md`](../audit-report.md), and the running API. Concrete divergences are flagged inline as **As-built** notes.

# 07.6 Claim Management API

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | API Design            |
| Document Number | 07.6                  |
| Document Name   | Claim Management API  |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

The Claim Management module is the core business domain of ClaimLens.

It manages the complete lifecycle of an insurance claim from submission through investigation and final resolution.

V1 supports:

```text
MOTOR INSURANCE CLAIMS
```

Primary Responsibilities:

* Claim Submission
* Claim Retrieval
* Claim Updates
* Claim Status Management
* Claim Timeline
* Claim Comments
* Claim Tags
* Claim Search
* Claim History Tracking
* Additional Information Requests
* Fraud Analysis Integration
* Assignment Integration
* Investigation Integration

---

# 2. Domain Entities

Managed Entities:

```text
claim
claim_history
claim_comment
claim_tag
claim_tag_mapping
```

Related Entities:

```text
customer
document
assignment
investigation
fraud_score
fraud_alert
claim_processing_state
```

---

# 3. Permission Matrix

| Permission           | Description         |
| -------------------- | ------------------- |
| CLAIM_VIEW           | View claims         |
| CLAIM_CREATE         | Create claims       |
| CLAIM_UPDATE         | Update claims       |
| CLAIM_DELETE         | Soft delete claims  |
| CLAIM_STATUS_UPDATE  | Change claim status |
| CLAIM_COMMENT_CREATE | Add comments        |
| CLAIM_TAG_MANAGE     | Manage tags         |
| CLAIM_EXPORT         | Export claims       |

**As-built (2026-07-29):** The real claim permission codes are `CLAIM_READ`, `CLAIM_WRITE`, `CLAIM_SUBMIT`, `CLAIM_ASSIGN`, `CLAIM_DECIDE`, and `CLAIM_INVESTIGATE`. The codes in this table (`CLAIM_VIEW/CREATE/UPDATE/DELETE/STATUS_UPDATE/COMMENT_CREATE/TAG_MANAGE/EXPORT`) do not exist — there are no comment, tag, or export permissions because those features were not built.

---

# 4. Claim Status Lifecycle

## Supported Statuses

```text
DRAFT
SUBMITTED
UNDER_REVIEW
PENDING_INFORMATION
ASSIGNED
INVESTIGATION_IN_PROGRESS
FRAUD_REVIEW
APPROVED
REJECTED
CLOSED
```

**As-built (2026-07-29):** The shipped `ClaimStatus` enum has **11** values: `DRAFT, SUBMITTED, AWAITING_ANALYSIS, AWAITING_ASSIGNMENT, AWAITING_ACCEPTANCE, UNDER_INVESTIGATION, WAITING_FOR_CUSTOMER, APPROVED, REJECTED, CLOSED, REOPENED`. The design names above (`UNDER_REVIEW`, `PENDING_INFORMATION`, `ASSIGNED`, `INVESTIGATION_IN_PROGRESS`, `FRAUD_REVIEW`) do not exist. `isTerminal()` = `APPROVED|REJECTED|CLOSED`. There is no `CANCELLED`. `REOPENED` exists in the enum but its transition is **not wired** — there is no reopen endpoint. Status is advanced only through the action endpoints (`/submit`, `/assign`, `/auto-assign`, `/reassign`, `/decision`, `/request-information`) plus the internal processing pipeline, not via a free-form status setter.

---

## Status Flow

```text
DRAFT
  ↓
SUBMITTED
  ↓
UNDER_REVIEW
  ↓
ASSIGNED
  ↓
INVESTIGATION_IN_PROGRESS
  ↓
FRAUD_REVIEW
  ↓
APPROVED / REJECTED
  ↓
CLOSED
```

---

# 5. Claim APIs

**As-built (2026-07-29):** The shipped claim endpoints (base `/api/v1`, tenant from JWT — there is no `{companyId}` path variable) are:
> - `POST /claims` (`CLAIM_WRITE`) · `GET /claims/{id}` (`CLAIM_READ`) · `GET /claims?page&size` → `PagedResponse` (`CLAIM_READ`). (Unlike `/customers`, `/users`, and `/policies`, the claims list has NO `/options` sibling.)
> - `POST /claims/{id}/submit` (`CLAIM_SUBMIT`) · `POST /claims/{id}/assign` (`CLAIM_ASSIGN`) · `POST /claims/{id}/auto-assign` (`CLAIM_ASSIGN`) · `POST /claims/{id}/reassign` (`CLAIM_ASSIGN`) · `POST /claims/{id}/decision` (`CLAIM_DECIDE`, approve/reject) · `POST /claims/{id}/request-information` (`CLAIM_INVESTIGATE`).
> - `GET /claims/{id}/processing` (`CLAIM_READ`) · `GET /claims/{id}/audit` (`AUDIT_READ`) · `POST|GET /claims/{id}/investigation-notes` (`CLAIM_INVESTIGATE` / `CLAIM_READ`) · the document sub-resources (see Document Management API).
>
> Endpoints described below that were NOT built: `PUT /claims/{id}` (update), `DELETE /claims/{id}`, `GET /claims/search`, `PATCH /claims/{id}/status`, `GET …/allowed-statuses`, `GET …/history` (read history via `…/audit`), comments, tags, `…/timeline` (staff), `…/summary`, and `GET /claims/export`.

---

## Create Claim

### Endpoint

```http
POST /api/v1/claims
```

### Permissions

```text
CLAIM_CREATE
```

**As-built (2026-07-29):** Permission is `CLAIM_WRITE`. A newly created claim starts in `DRAFT` (not `SUBMITTED`); the processing pipeline is triggered by the separate `POST /claims/{id}/submit` (`CLAIM_SUBMIT`), which moves it `SUBMITTED → AWAITING_ANALYSIS → …`. The `Idempotency-Key` header is not honored.

### Headers

```http
Idempotency-Key: uuid
```

### Request Body

```json
{
  "claimTypeId": 1,
  "customerId": 5001,
  "policyNumber": "POL123456",
  "vehicleRegistrationNumber": "DL01AB1234",
  "incidentDate": "2026-06-01",
  "incidentLocation": "Delhi",
  "estimatedLossAmount": 250000,
  "description": "Vehicle collision at traffic intersection"
}
```

### Validation Rules

| Field               | Rules                 |
| ------------------- | --------------------- |
| claimTypeId         | Required              |
| customerId          | Required              |
| policyNumber        | Required              |
| incidentDate        | Cannot be future date |
| estimatedLossAmount | Greater than 0        |

### Business Rules

* Claim number generated automatically.
* Claim starts in SUBMITTED status.
* Initial claim history record created.
* Processing workflow automatically initiated.

### Success Response

```http
201 Created
```

```json
{
  "claimId": 10001,
  "claimNumber": "CLM-2026-000001",
  "status": "SUBMITTED"
}
```

### Audit Event

```text
CLAIM_CREATED
```

---

## Get Claim

### Endpoint

```http
GET /api/v1/claims/{claimId}
```

### Permissions

```text
CLAIM_VIEW
```

### Success Response

```json
{
  "claimId": 10001,
  "claimNumber": "CLM-2026-000001",
  "claimType": "MOTOR",
  "customer": {
    "customerId": 5001,
    "customerNumber": "CUS00001",
    "name": "Rahul Sharma"
  },
  "status": "UNDER_REVIEW",
  "fraudScore": 45,
  "createdAt": "2026-06-01T10:00:00Z"
}
```

---

## Update Claim

**As-built (2026-07-29):** Not built — there is no `PUT /claims/{id}`.

### Endpoint

```http
PUT /api/v1/claims/{claimId}
```

### Permissions

```text
CLAIM_UPDATE
```

### Request Body

```json
{
  "incidentLocation": "Noida",
  "description": "Updated description"
}
```

### Business Rules

* Approved claims cannot be updated.
* Closed claims cannot be updated.
* History record generated.

### Success Response

```json
{
  "message": "Claim updated successfully"
}
```

### Audit Event

```text
CLAIM_UPDATED
```

---

## Delete Claim

**As-built (2026-07-29):** Not built — there is no `DELETE /claims/{id}`.

### Endpoint

```http
DELETE /api/v1/claims/{claimId}
```

### Description

Soft delete claim.

### Business Rules

* Only DRAFT claims may be deleted.
* Claims with investigations cannot be deleted.

### Success Response

```http
204 No Content
```

### Audit Event

```text
CLAIM_DELETED
```

---

# 6. Claim Listing APIs

---

## List Claims

### Endpoint

```http
GET /api/v1/claims
```

### Query Parameters

```text
page
size
sort
status
claimTypeId
customerId
assignedUserId
claimNumber
fraudRiskLevel
createdFrom
createdTo
```

### Example

```http
GET /api/v1/claims?status=UNDER_REVIEW
```

**As-built (2026-07-29):** `GET /claims?page&size` requires `CLAIM_READ` and returns the `{success, data}` envelope with `data` = `PagedResponse<T>` (`{content, page, size, totalElements, totalPages, first, last}` — booleans `first`/`last`, not `hasNext`/`hasPrevious`). `size` is clamped to 1..100. The only query params are `page` (default 0) and `size` (default 10, clamped to 1..100); the rich filter/sort set below (status/claimTypeId/customerId/assignedUserId/fraudRiskLevel/date range) is not implemented. The claims list has no `/options` sibling (only `/customers`, `/users`, `/policies` do).

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

## Search Claims

**As-built (2026-07-29):** Not built — there is no `GET /claims/search` endpoint.

### Endpoint

```http
GET /api/v1/claims/search
```

### Query Parameters

```text
q
```

### Searchable Fields

```text
claimNumber
policyNumber
vehicleRegistrationNumber
customerName
customerNumber
```

### Example

```http
GET /api/v1/claims/search?q=CLM-2026
```

---

# 7. Claim Status APIs

---

## Update Claim Status

**As-built (2026-07-29):** Not built as a generic status setter. There is no `PATCH /claims/{id}/status`. Status changes happen through purpose-built action endpoints — `/submit`, `/assign`, `/auto-assign`, `/reassign`, `/decision` (`CLAIM_DECIDE`, for approve/reject), `/request-information` — plus the internal processing pipeline.

### Endpoint

```http
PATCH /api/v1/claims/{claimId}/status
```

### Permissions

```text
CLAIM_STATUS_UPDATE
```

### Request Body

```json
{
  "status": "UNDER_REVIEW",
  "reason": "Documents received"
}
```

### Business Rules

* Only valid transitions allowed.
* Transition recorded in history.
* Audit event generated.

### Success Response

```json
{
  "claimId": 10001,
  "status": "UNDER_REVIEW"
}
```

### Audit Event

```text
CLAIM_STATUS_CHANGED
```

---

## Get Allowed Status Transitions

**As-built (2026-07-29):** Not built — there is no `GET /claims/{id}/allowed-statuses`.

### Endpoint

```http
GET /api/v1/claims/{claimId}/allowed-statuses
```

### Success Response

```json
[
  "ASSIGNED",
  "PENDING_INFORMATION"
]
```

---

# 8. Claim History APIs

---

## Get Claim History

**As-built (2026-07-29):** There is no `GET /claims/{id}/history`. Claim history/audit is read via `GET /api/v1/claims/{id}/audit` (permission `AUDIT_READ`).

### Endpoint

```http
GET /api/v1/claims/{claimId}/history
```

### Description

Retrieve complete claim timeline.

### Success Response

```json
[
  {
    "historyId": 1,
    "eventType": "STATUS_CHANGED",
    "oldValue": "SUBMITTED",
    "newValue": "UNDER_REVIEW",
    "createdBy": "EMP0001",
    "createdAt": "2026-06-01T11:00:00Z"
  }
]
```

---

# 9. Claim Comment APIs

**As-built (2026-07-29):** Not built. The comment APIs (section 9) and the tag APIs (section 10) do not exist — there are no `claim_comment`, `claim_tag`, or `claim_tag_mapping` tables or endpoints. Free-text notes on a claim are handled instead by investigation-notes: `POST /claims/{id}/investigation-notes` (`CLAIM_INVESTIGATE`) and `GET /claims/{id}/investigation-notes` (`CLAIM_READ`).

---

## Add Comment

### Endpoint

```http
POST /api/v1/claims/{claimId}/comments
```

### Permissions

```text
CLAIM_COMMENT_CREATE
```

### Request Body

```json
{
  "comment": "Documents verified successfully."
}
```

### Success Response

```json
{
  "commentId": 101
}
```

### Audit Event

```text
CLAIM_COMMENT_ADDED
```

---

## Get Comments

### Endpoint

```http
GET /api/v1/claims/{claimId}/comments
```

### Success Response

```json
[
  {
    "commentId": 101,
    "comment": "Documents verified successfully.",
    "createdBy": "EMP0001",
    "createdAt": "2026-06-01T12:00:00Z"
  }
]
```

---

# 10. Claim Tag APIs

---

## Create Tag

### Endpoint

```http
POST /api/v1/claims/tags
```

### Request Body

```json
{
  "tagName": "HIGH_PRIORITY"
}
```

### Success Response

```json
{
  "tagId": 1
}
```

### Audit Event

```text
CLAIM_TAG_CREATED
```

---

## Assign Tag To Claim

### Endpoint

```http
POST /api/v1/claims/{claimId}/tags
```

### Request Body

```json
{
  "tagId": 1
}
```

### Success Response

```json
{
  "message": "Tag assigned successfully"
}
```

### Audit Event

```text
CLAIM_TAG_ASSIGNED
```

---

## Remove Tag

### Endpoint

```http
DELETE /api/v1/claims/{claimId}/tags/{tagId}
```

### Success Response

```http
204 No Content
```

### Audit Event

```text
CLAIM_TAG_REMOVED
```

---

## Get Claim Tags

### Endpoint

```http
GET /api/v1/claims/{claimId}/tags
```

---

# 11. Additional Information APIs

---

## Request Additional Information

**As-built (2026-07-29):** The endpoint is `POST /api/v1/claims/{id}/request-information` (permission `CLAIM_INVESTIGATE`), which moves the claim to `WAITING_FOR_CUSTOMER`. There is no `/information-requests` collection, no `GET …/information-requests`, and no `…/information-requests/{requestId}/submit` — the customer responds through the customer portal.

### Endpoint

```http
POST /api/v1/claims/{claimId}/information-requests
```

### Request Body

```json
{
  "requestTitle": "Additional Vehicle Images",
  "requestMessage": "Please upload rear damage photos.",
  "dueDate": "2026-06-10"
}
```

### Business Rules

* Claim status changes to PENDING_INFORMATION.
* Notification generated.

### Success Response

```json
{
  "requestId": 1001
}
```

### Audit Event

```text
INFORMATION_REQUEST_CREATED
```

---

## Get Information Requests

### Endpoint

```http
GET /api/v1/claims/{claimId}/information-requests
```

---

## Submit Additional Information

### Endpoint

```http
POST /api/v1/claims/{claimId}/information-requests/{requestId}/submit
```

### Headers

```http
Idempotency-Key: uuid
```

### Request Body

```json
{
  "responseMessage": "Requested images uploaded."
}
```

### Success Response

```json
{
  "message": "Information submitted successfully"
}
```

### Audit Event

```text
INFORMATION_REQUEST_RESPONDED
```

---

# 12. Claim Timeline API

**As-built (2026-07-29):** No staff-facing `GET /claims/{id}/timeline`. Staff read the unified history via `GET /claims/{id}/audit` (`AUDIT_READ`). A timeline endpoint exists only on the customer portal: `GET /api/v1/portal/claims/{id}/timeline`.

---

## Get Claim Timeline

### Endpoint

```http
GET /api/v1/claims/{claimId}/timeline
```

### Description

Unified timeline of:

* Status Changes
* Comments
* Assignments
* Investigation Events
* Fraud Events
* Document Uploads

### Success Response

```json
[
  {
    "eventType": "CLAIM_CREATED",
    "timestamp": "2026-06-01T10:00:00Z"
  },
  {
    "eventType": "DOCUMENT_UPLOADED",
    "timestamp": "2026-06-01T10:10:00Z"
  }
]
```

---

# 13. Claim Summary API

**As-built (2026-07-29):** Not built — there is no `GET /claims/{id}/summary`.

---

## Get Claim Summary

### Endpoint

```http
GET /api/v1/claims/{claimId}/summary
```

### Purpose

Optimized dashboard view.

### Success Response

```json
{
  "claimId": 10001,
  "claimNumber": "CLM-2026-000001",
  "status": "UNDER_REVIEW",
  "fraudScore": 45,
  "documentCount": 8,
  "investigationStatus": "OPEN",
  "assignedInvestigator": "John Doe"
}
```

---

# 14. Export APIs

**As-built (2026-07-29):** Not built — there is no `GET /claims/export` and no `CLAIM_EXPORT` permission.

---

## Export Claims

### Endpoint

```http
GET /api/v1/claims/export
```

### Permission

```text
CLAIM_EXPORT
```

### Query Parameters

Same filters as claim listing.

### Formats

```text
CSV
XLSX
```

### Success Response

```http
202 Accepted
```

### Audit Event

```text
CLAIMS_EXPORTED
```

---

# 15. Business Validation Rules

## Claim Creation

* Customer must exist.
* Claim type must exist.
* Incident date cannot be future date.
* Policy number required.
* Tenant isolation enforced.

## Claim Updates

* Closed claims cannot be modified.
* Approved claims cannot be modified.
* Fraud review claims require manager approval.

## Status Management

* Status transitions must follow workflow.
* Invalid transitions rejected.

## Information Requests

* Due date must be future date.
* Open request must not already exist.

---

# 16. Error Codes

| Error Code                      | Description          |
| ------------------------------- | -------------------- |
| CLAIM_NOT_FOUND                 | Claim not found      |
| INVALID_STATUS_TRANSITION       | Invalid transition   |
| CLAIM_ALREADY_CLOSED            | Claim closed         |
| CLAIM_ALREADY_APPROVED          | Claim approved       |
| CUSTOMER_NOT_FOUND              | Customer missing     |
| CLAIM_TYPE_NOT_FOUND            | Claim type missing   |
| INFORMATION_REQUEST_NOT_FOUND   | Request missing      |
| DUPLICATE_TAG                   | Tag already assigned |
| OPEN_INFORMATION_REQUEST_EXISTS | Request already open |
| CLAIM_HAS_ACTIVE_INVESTIGATION  | Deletion blocked     |

---

# 17. Audit Events

```text
CLAIM_CREATED
CLAIM_UPDATED
CLAIM_DELETED

CLAIM_STATUS_CHANGED

CLAIM_COMMENT_ADDED

CLAIM_TAG_CREATED
CLAIM_TAG_ASSIGNED
CLAIM_TAG_REMOVED

INFORMATION_REQUEST_CREATED
INFORMATION_REQUEST_RESPONDED

CLAIMS_EXPORTED
```

---

# 18. Integration Events

The Claim module triggers:

```text
CLAIM_SUBMITTED
CLAIM_UPDATED
CLAIM_ASSIGNED
CLAIM_STATUS_CHANGED
CLAIM_INFORMATION_REQUESTED
```

Consumed by:

* Assignment Engine
* Processing Engine
* Fraud Engine
* Notification Service
* Analytics Service

---

# 19. Performance Requirements

| API            | Target   |
| -------------- | -------- |
| Create Claim   | < 500 ms |
| Claim Lookup   | < 200 ms |
| Claim Search   | < 300 ms |
| Claim Timeline | < 500 ms |
| Status Update  | < 300 ms |
| Export Request | < 500 ms |

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

* PII masking in logs
* Export authorization checks
* Cross-tenant access prevention
* Status transition authorization enforcement

---

# Approval

This document defines the Claim Management API contract for ClaimLens and serves as the implementation reference for claim lifecycle management, claim workflows, history tracking, comments, tags, information requests, and claim-related integrations.
