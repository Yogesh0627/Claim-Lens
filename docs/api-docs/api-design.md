# 07.1 API Standards

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | API Design            |
| Document Number | 07.1                  |
| Document Name   | API Standards         |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Purpose

This document defines the global API standards for all ClaimLens REST APIs.

The purpose of this document is to ensure:

* Consistent API behavior
* Predictable request and response structures
* Strong security controls
* Tenant isolation
* Auditable operations
* Simplified frontend integration
* Easier future microservice extraction

All APIs in ClaimLens must comply with the standards defined in this document.

---

# 2. API Architecture Principles

ClaimLens follows:

* RESTful API Design
* Resource-Oriented Endpoints
* Stateless Communication
* JSON Request/Response Payloads
* JWT Authentication
* Tenant-Aware Access Control
* Idempotent Critical Operations
* Audit Logging for Mutations

---

# 3. Base URL Structure

## Development

```http
http://localhost:8080/api/v1
```

## Staging

```http
https://staging-api.claimlens.com/api/v1
```

## Production

```http
https://api.claimlens.com/api/v1
```

---

# 4. API Versioning

Versioning shall be URL based.

Example:

```http
/api/v1/claims
```

Future versions:

```http
/api/v2/claims
```

Rules:

* Breaking changes require new version.
* Non-breaking changes may remain in current version.
* Deprecated endpoints remain available for at least one release cycle.

---

# 5. Content Types

## Request

```http
Content-Type: application/json
```

## Response

```http
Content-Type: application/json
```

## Multipart Upload

```http
Content-Type: multipart/form-data
```

Used for:

* Document Upload
* Bulk Import
* Investigation Attachments

---

# 6. Authentication

Authentication uses JWT Bearer Tokens.

Header:

```http
Authorization: Bearer <jwt-token>
```

Example:

```http
Authorization: Bearer eyJhbGciOiJIUzI1Ni...
```

Rules:

* Required for all protected APIs.
* JWT validated by Spring Security.
* Expired tokens return 401.
* Invalid tokens return 401.

---

# 7. Tenant Isolation

ClaimLens uses a shared database architecture.

Every request must include:

```http
X-Tenant-Id: tenant-id
```

Example:

```http
X-Tenant-Id: 101
```

Validation:

* User must belong to tenant.
* Requested resources must belong to tenant.
* Cross-tenant access is prohibited.

Response:

```http
403 Forbidden
```

if tenant mismatch occurs.

---

# 8. Correlation ID

Every request must contain:

```http
X-Correlation-Id
```

Example:

```http
X-Correlation-Id: 9a3a6a1f-6d9d-4c54-b9d5-5fdedca0919f
```

Purpose:

* Distributed tracing
* Log correlation
* Audit tracking
* Incident investigations

If absent:

Backend generates one automatically.

---

# 9. Standard Request Headers

| Header           | Required    | Description              |
| ---------------- | ----------- | ------------------------ |
| Authorization    | Yes         | JWT Token                |
| X-Tenant-Id      | Yes         | Tenant Identifier        |
| X-Correlation-Id | No          | Request Tracking         |
| Content-Type     | Yes         | Payload Type             |
| Idempotency-Key  | Conditional | Critical POST Operations |

---

# 10. Standard Response Envelope

Successful responses may return resource objects directly.

Example:

```json
{
  "claimId": 1001,
  "claimNumber": "CLM-2026-000001"
}
```

Collection responses shall use pagination format.

---

# 11. Pagination Standard

Request:

```http
GET /claims?page=0&size=20
```

Defaults:

| Property | Value |
| -------- | ----- |
| page     | 0     |
| size     | 20    |

Maximum:

```text
100
```

Response:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 120,
  "totalPages": 6,
  "hasNext": true,
  "hasPrevious": false
}
```

---

# 12. Sorting Standard

Request:

```http
GET /claims?sort=createdAt,desc
```

Multiple Sorts:

```http
GET /claims?sort=status,asc&sort=createdAt,desc
```

Allowed Directions:

```text
asc
desc
```

---

# 13. Filtering Standard

Examples:

```http
GET /claims?status=OPEN
```

```http
GET /claims?claimType=MOTOR
```

```http
GET /claims?assignedUserId=100
```

Multiple filters may be combined.

---

# 14. Search Standard

Example:

```http
GET /claims/search?q=vehicle damage
```

Supported Resources:

* Claims
* Customers
* Investigations
* Documents

Search behavior:

* Case-insensitive
* Partial matching
* Tenant scoped

---

# 15. Date Format Standard

All dates shall use ISO-8601.

Date:

```json
"2026-06-01"
```

Timestamp:

```json
"2026-06-01T14:20:35Z"
```

Timezone:

```text
UTC
```

Mandatory for all backend services.

---

# 16. Resource Naming Convention

Resources use plural nouns.

Examples:

```http
/claims
/users
/documents
/investigations
```

Avoid:

```http
/getClaims
/createUser
```

---

# 17. HTTP Method Standards

| Method | Purpose        |
| ------ | -------------- |
| GET    | Read           |
| POST   | Create         |
| PUT    | Full Update    |
| PATCH  | Partial Update |
| DELETE | Soft Delete    |

---

# 18. Standard Success Codes

| Code | Meaning    |
| ---- | ---------- |
| 200  | OK         |
| 201  | Created    |
| 202  | Accepted   |
| 204  | No Content |

---

# 19. Standard Error Codes

| Code | Meaning             |
| ---- | ------------------- |
| 400  | Bad Request         |
| 401  | Unauthorized        |
| 403  | Forbidden           |
| 404  | Not Found           |
| 409  | Conflict            |
| 422  | Validation Failure  |
| 429  | Too Many Requests   |
| 500  | Internal Error      |
| 503  | Service Unavailable |

---

# 20. Error Response Structure

```json
{
  "timestamp": "2026-06-01T12:00:00Z",
  "correlationId": "9a3a6a1f",
  "errorCode": "CLAIM_NOT_FOUND",
  "message": "Claim not found",
  "path": "/api/v1/claims/1001"
}
```

---

# 21. Validation Error Response

```json
{
  "timestamp": "2026-06-01T12:00:00Z",
  "correlationId": "9a3a6a1f",
  "errorCode": "VALIDATION_FAILED",
  "message": "Validation failed",
  "errors": [
    {
      "field": "claimTypeId",
      "message": "Claim Type is required"
    }
  ]
}
```

---

# 22. Idempotency Standard

Critical create operations shall support:

```http
Idempotency-Key
```

Example:

```http
Idempotency-Key: c5f68f88-4f8a-4c10-baf2-91fd6a57ab12
```

Applicable APIs:

* Claim Submission
* Document Upload
* Investigation Report Submission
* Additional Information Submission

Duplicate requests with same key return original response.

---

# 23. Rate Limiting

Default limits:

| API Type    | Limit   |
| ----------- | ------- |
| Read APIs   | 500/min |
| Write APIs  | 100/min |
| Upload APIs | 20/min  |

Exceeded requests return:

```http
429 Too Many Requests
```

---

# 24. File Upload Standards

Supported Types:

```text
PDF
PNG
JPG
JPEG
TIFF
```

Maximum File Size:

```text
50 MB
```

Virus scanning required before processing.

Files stored in:

```text
AWS S3
```

Metadata stored in PostgreSQL.

---

# 25. Security Standards

All APIs must enforce:

* Authentication
* Authorization
* Tenant Isolation
* Input Validation
* SQL Injection Protection
* XSS Protection
* CSRF Protection (if applicable)
* Secure Headers

Sensitive fields must never be returned:

* Password Hashes
* Internal Tokens
* Secrets
* Encryption Keys

---

# 26. Audit Logging Standards

All mutation APIs must generate audit events.

Events:

```text
CREATE
UPDATE
DELETE
STATUS_CHANGE
ASSIGNMENT_CHANGE
LOGIN
LOGOUT
EXPORT
DOWNLOAD
```

Captured Data:

* User
* Tenant
* Timestamp
* Entity Type
* Entity ID
* Previous Value
* New Value

Stored in:

```text
audit.audit_log
```

---

# 27. Observability Standards

All APIs must expose metrics for:

* Request Count
* Error Count
* Latency
* Throughput
* Active Connections
* Database Pool Usage

Prometheus metrics endpoint:

```http
/actuator/prometheus
```

---

# 28. Logging Standards

Every request log must include:

* Correlation ID
* Tenant ID
* User ID
* Endpoint
* HTTP Method
* Status Code
* Duration

Sensitive data must be masked.

Examples:

* Passwords
* Access Tokens
* PII Fields

---

# 29. OpenAPI Standards

Every endpoint shall be documented using:

```text
OpenAPI 3.1
```

Generated through:

```text
springdoc-openapi
```

Swagger UI:

```http
/swagger-ui.html
```

---

# 30. Future Compatibility

API contracts shall support future migration to:

* Microservices
* Event-Driven Architecture
* Mobile Applications
* External Partner APIs

without breaking existing consumers.

---

# Approval

This document serves as the authoritative API standard for all ClaimLens services and modules.
