# 07.15 Audit & Compliance API

## Document Information

| Field           | Value                  |
| --------------- | ---------------------- |
| Project         | ClaimLens              |
| Company         | Niyo Technologies  |
| Version         | V1                     |
| Document Type   | API Design             |
| Document Number | 07.15                  |
| Document Name   | Audit & Compliance API |
| Status          | Approved               |
| Last Updated    | June 2026              |

---

# 1. Overview

The Audit & Compliance Module provides a complete, immutable audit trail for all critical actions performed within ClaimLens.

The module is designed to support:

* Regulatory Compliance
* Internal Audits
* Security Investigations
* User Activity Tracking
* Data Change Tracking
* Operational Monitoring
* Fraud Investigation Support
* Legal Discovery Requests
* Tenant Compliance Reporting

Every significant business event within ClaimLens generates audit records.

---

# 2. Domain Entities

Managed Entities:

```text
audit_event
audit_log
```

Related Entities:

```text
user
claim
document
assignment
investigation
fraud_alert
notification
```

---

# 3. Audit Architecture

## Audit Flow

```text
User Action
      ↓
Business Service
      ↓
Audit Event Generated
      ↓
Audit Event Stored
      ↓
Audit Log Created
      ↓
Search & Reporting APIs
```

---

## Audit Design Principles

Audit records are:

```text
IMMUTABLE
APPEND_ONLY
TIMESTAMPED
TENANT_SCOPED
USER_TRACEABLE
```

Audit records are never updated.

Audit records are never deleted.

---

# 4. Permission Matrix

| Permission          | Description             |
| ------------------- | ----------------------- |
| AUDIT_VIEW          | View audit records      |
| AUDIT_EXPORT        | Export audit reports    |
| COMPLIANCE_VIEW     | View compliance reports |
| SECURITY_AUDIT_VIEW | View security events    |
| AUDIT_ADMIN         | Audit administration    |

---

# 5. Audit Event APIs

---

## Get Audit Event

### Endpoint

```http
GET /api/v1/audit/events/{eventId}
```

### Permissions

```text
AUDIT_VIEW
```

### Success Response

```json
{
  "eventId": 1001,
  "eventType": "CLAIM_CREATED",
  "entityType": "CLAIM",
  "entityId": 10001,
  "performedBy": "EMP0001",
  "createdAt": "2026-06-01T10:00:00Z"
}
```

---

## List Audit Events

### Endpoint

```http
GET /api/v1/audit/events
```

### Query Parameters

```text
page
size
eventType
entityType
performedBy
fromDate
toDate
```

### Success Response

```json
{
  "content": [],
  "totalElements": 0
}
```

---

# 6. Audit Log APIs

---

## Get Audit Log

### Endpoint

```http
GET /api/v1/audit/logs/{logId}
```

### Success Response

```json
{
  "logId": 5001,
  "eventType": "CLAIM_UPDATED",
  "oldValues": {
    "status": "SUBMITTED"
  },
  "newValues": {
    "status": "UNDER_REVIEW"
  }
}
```

---

## Search Audit Logs

### Endpoint

```http
GET /api/v1/audit/logs/search
```

### Query Parameters

```text
entityType
entityId
userId
eventType
fromDate
toDate
```

### Success Response

```json
{
  "content": [],
  "totalElements": 0
}
```

---

# 7. Entity History APIs

---

## Get Claim Audit History

### Endpoint

```http
GET /api/v1/audit/claims/{claimId}/history
```

### Success Response

```json
[
  {
    "eventType": "CLAIM_CREATED",
    "timestamp": "2026-06-01T10:00:00Z"
  },
  {
    "eventType": "CLAIM_STATUS_CHANGED",
    "timestamp": "2026-06-01T11:00:00Z"
  }
]
```

---

## Get Document Audit History

### Endpoint

```http
GET /api/v1/audit/documents/{documentId}/history
```

---

## Get Investigation Audit History

### Endpoint

```http
GET /api/v1/audit/investigations/{investigationId}/history
```

---

## Get Fraud Audit History

### Endpoint

```http
GET /api/v1/audit/fraud/{claimId}/history
```

---

# 8. User Activity APIs

---

## Get User Activity

### Endpoint

```http
GET /api/v1/audit/users/{userId}/activity
```

### Success Response

```json
{
  "userId": 1001,
  "activityCount": 1542,
  "lastActivityAt": "2026-06-01T15:00:00Z"
}
```

---

## User Activity Timeline

### Endpoint

```http
GET /api/v1/audit/users/{userId}/timeline
```

### Success Response

```json
[
  {
    "eventType": "LOGIN_SUCCESS",
    "timestamp": "2026-06-01T09:00:00Z"
  },
  {
    "eventType": "CLAIM_UPDATED",
    "timestamp": "2026-06-01T10:00:00Z"
  }
]
```

---

# 9. Security Audit APIs

---

## Security Events

### Endpoint

```http
GET /api/v1/audit/security/events
```

### Permissions

```text
SECURITY_AUDIT_VIEW
```

### Query Parameters

```text
eventType
severity
fromDate
toDate
```

### Success Response

```json
[
  {
    "eventType": "LOGIN_FAILED",
    "severity": "HIGH",
    "timestamp": "2026-06-01T12:00:00Z"
  }
]
```

---

## Failed Login Report

### Endpoint

```http
GET /api/v1/audit/security/failed-logins
```

### Success Response

```json
{
  "failedLoginAttempts": 45
}
```

---

## Permission Change Report

### Endpoint

```http
GET /api/v1/audit/security/permission-changes
```

### Success Response

```json
[
  {
    "userId": 1001,
    "changeType": "ROLE_ASSIGNED"
  }
]
```

---

# 10. Compliance Reporting APIs

---

## Compliance Summary

### Endpoint

```http
GET /api/v1/audit/compliance/summary
```

### Permissions

```text
COMPLIANCE_VIEW
```

### Success Response

```json
{
  "auditEvents": 1250000,
  "securityEvents": 5200,
  "complianceViolations": 12
}
```

---

## Compliance Violations

### Endpoint

```http
GET /api/v1/audit/compliance/violations
```

### Success Response

```json
[
  {
    "violationId": 101,
    "violationType": "UNAUTHORIZED_ACCESS"
  }
]
```

---

## Data Access Report

### Endpoint

```http
GET /api/v1/audit/compliance/data-access
```

### Query Parameters

```text
entityType
entityId
```

### Success Response

```json
[
  {
    "accessedBy": "EMP0001",
    "accessedAt": "2026-06-01T12:00:00Z"
  }
]
```

---

# 11. Audit Export APIs

---

## Export Audit Report

### Endpoint

```http
POST /api/v1/audit/export
```

### Permissions

```text
AUDIT_EXPORT
```

### Request Body

```json
{
  "reportType": "CLAIM_HISTORY",
  "fromDate": "2026-06-01",
  "toDate": "2026-06-30",
  "format": "XLSX"
}
```

### Success Response

```json
{
  "exportJobId": 9001
}
```

### Audit Event

```text
AUDIT_EXPORT_REQUESTED
```

---

## Get Export Status

### Endpoint

```http
GET /api/v1/audit/export/{exportJobId}
```

### Success Response

```json
{
  "status": "COMPLETED",
  "downloadUrl": "https://presigned-url"
}
```

---

# 12. Audit Dashboard APIs

---

## Audit Dashboard Summary

### Endpoint

```http
GET /api/v1/audit/dashboard
```

### Success Response

```json
{
  "totalAuditEvents": 1250000,
  "todayAuditEvents": 5200,
  "securityEvents": 125,
  "failedLogins": 42
}
```

---

## Audit Trends

### Endpoint

```http
GET /api/v1/audit/dashboard/trends
```

### Success Response

```json
{
  "dailyAuditEvents": [],
  "dailySecurityEvents": []
}
```

---

# 13. Audit Event Catalog

## Authentication Events

```text
LOGIN_SUCCESS
LOGIN_FAILED
LOGOUT
PASSWORD_RESET_REQUESTED
PASSWORD_RESET_COMPLETED
```

---

## User Events

```text
USER_CREATED
USER_UPDATED
USER_DEACTIVATED
ROLE_ASSIGNED
ROLE_REMOVED
```

---

## Claim Events

```text
CLAIM_CREATED
CLAIM_UPDATED
CLAIM_STATUS_CHANGED
CLAIM_DELETED
```

---

## Document Events

```text
DOCUMENT_UPLOADED
DOCUMENT_VERSION_CREATED
DOCUMENT_DELETED
```

---

## Assignment Events

```text
ASSIGNMENT_CREATED
CLAIM_REASSIGNED
```

---

## Investigation Events

```text
INVESTIGATION_CREATED
INVESTIGATION_COMPLETED
INVESTIGATION_CLOSED
```

---

## Fraud Events

```text
FRAUD_EVALUATION_REQUESTED
FRAUD_SCORE_OVERRIDDEN
FRAUD_ALERT_CLOSED
```

---

## Administrative Events

```text
POLICY_UPDATED
CONFIGURATION_CHANGED
ANALYTICS_AGGREGATION_TRIGGERED
```

---

# 14. Compliance Requirements

## Audit Retention

Retention Policy:

```text
Audit Events      : 7 Years
Security Events   : 7 Years
Compliance Reports: 7 Years
```

---

## Immutability Rules

Audit records:

```text
CANNOT BE UPDATED
CANNOT BE DELETED
CANNOT BE OVERWRITTEN
```

---

## Tenant Isolation

All audit records include:

```text
tenant_id
```

for tenant-level segregation.

---

# 15. Business Validation Rules

## Audit Records

* Generated automatically.
* Immutable after creation.
* Timestamp mandatory.

## Compliance Reports

* Date range required.
* Tenant isolation enforced.

## Exports

* Export actions audited.
* Download links expire after 24 hours.

---

# 16. Error Codes

| Error Code                  | Description    |
| --------------------------- | -------------- |
| AUDIT_EVENT_NOT_FOUND       | Event missing  |
| AUDIT_LOG_NOT_FOUND         | Log missing    |
| COMPLIANCE_REPORT_NOT_FOUND | Report missing |
| EXPORT_JOB_NOT_FOUND        | Export missing |
| INVALID_DATE_RANGE          | Invalid range  |
| AUDIT_ACCESS_DENIED         | Access denied  |

---

# 17. Audit Events

The Audit Module records events generated by every module across ClaimLens.

Special audit events:

```text
AUDIT_EXPORT_REQUESTED
COMPLIANCE_REPORT_GENERATED
SECURITY_REPORT_GENERATED
AUDIT_SEARCH_EXECUTED
```

---

# 18. Integration Events

Consumed From:

```text
AUTH MODULE
ORGANIZATION MODULE
USER MODULE
CLAIM MODULE
DOCUMENT MODULE
ASSIGNMENT MODULE
INVESTIGATION MODULE
PROCESSING MODULE
FRAUD MODULE
NOTIFICATION MODULE
ANALYTICS MODULE
```

Produces:

```text
AUDIT_EVENT_CREATED
AUDIT_LOG_CREATED
```

---

# 19. Performance Requirements

| API                | Target   |
| ------------------ | -------- |
| Audit Lookup       | < 300 ms |
| Audit Search       | < 500 ms |
| User Timeline      | < 500 ms |
| Compliance Summary | < 300 ms |
| Audit Dashboard    | < 300 ms |
| Export Request     | < 500 ms |

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

* Security audit APIs restricted to administrators.
* Compliance APIs restricted to compliance officers.
* Exports fully auditable.
* Cross-tenant audit access prohibited.

---

# Approval

This document defines the Audit & Compliance API contract for ClaimLens and serves as the implementation reference for audit logging, compliance reporting, security event tracking, user activity monitoring, regulatory reporting, and immutable audit history management.
