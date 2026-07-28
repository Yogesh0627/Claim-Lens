> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; the authoritative behaviour is the service code. Where this diverges, the code wins ([`../architecture.md`](../architecture.md), [`../domain-model.md`](../domain-model.md), [`../audit-report.md`](../audit-report.md)); deltas flagged inline as **As-built** notes.

# 09.10 Audit Service Design

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | Service Design        |
| Document Number | 09.10                 |
| Document Name   | Audit Service Design  |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

The Audit Module is responsible for maintaining immutable audit trails across the entire platform.

Responsibilities:

* Audit Event Recording
* Audit Log Persistence
* User Activity Tracking
* Security Event Tracking
* Compliance Reporting
* Audit Export Processing
* Regulatory Support

The Audit Module is the system of record for operational history.

**As-built (2026-07-29):** the built module is a single **`AuditLog`** entity + `AuditService` with two methods: `record(...)` (insert) and `getClaimAudit(claimId)` (read). Recording is **not** event/outbox-driven (no outbox exists — see `service-design.md` §11): an **`@Auditable` AOP aspect** intercepts annotated service methods and calls `AuditService.record` **synchronously within the same business transaction** (`Propagation.REQUIRED`), so a rolled-back action leaves no audit row. `AuditEvent`, `ComplianceReport`, `AuditExportJob` and `AuditAttachment` were **dropped**, along with `SecurityAuditService` / `ComplianceService` / `AuditExportService`.

---

# 2. Module Dependencies

## Consumed Events

```text
CLAIM_CREATED

CLAIM_UPDATED

DOCUMENT_UPLOADED

ASSIGNMENT_CREATED

INVESTIGATION_COMPLETED

OCR_COMPLETED

ANALYSIS_COMPLETED

FRAUD_ALERT_CREATED

NOTIFICATION_DELIVERED

REPORT_EXPORT_COMPLETED
```

---

## Published Events

```text
AUDIT_EVENT_CREATED

AUDIT_LOG_CREATED

USER_ACTIVITY_RECORDED

SECURITY_EVENT_RECORDED

COMPLIANCE_REPORT_GENERATED

AUDIT_EXPORT_REQUESTED

AUDIT_EXPORT_COMPLETED

AUDIT_EXPORT_FAILED
```

---

# 3. Package Structure

```text
audit

├── controller
│   └── AuditController

├── service
│   ├── AuditService
│   ├── SecurityAuditService
│   ├── ComplianceService
│   ├── AuditExportService
│   └── AuditServiceImpl

├── repository
│   ├── AuditEventRepository
│   ├── AuditLogRepository
│   ├── ComplianceReportRepository
│   └── AuditExportRepository

├── entity
│   ├── AuditEvent
│   ├── AuditLog
│   ├── ComplianceReport
│   └── AuditExportJob

├── dto

├── mapper

├── validator

├── event

├── worker

├── exception
```

---

# 4. Domain Entities

Primary Entities:

```text
AuditEvent

AuditLog

ComplianceReport

AuditExportJob
```

---

# 5. Audit Service

Responsibilities:

```text
Record Audit Events

Persist Audit Logs

Retrieve Audit History

Audit Search
```

---

## Methods

```java
recordEvent()

recordAuditLog()

getAuditHistory()

searchAuditLogs()
```

---

# 6. Security Audit Service

Responsibilities:

```text
Track Security Events

Failed Login Tracking

Permission Violation Tracking

Account Lock Monitoring
```

---

## Security Event Types

```text
LOGIN_FAILED

LOGIN_SUCCESS

ROLE_CHANGED

ACCOUNT_LOCKED

PERMISSION_DENIED

PASSWORD_RESET
```

---

## Methods

```java
recordSecurityEvent()

getSecurityEvents()
```

---

# 7. Compliance Service

Responsibilities:

```text
Generate Compliance Reports

Regulatory Reporting

Retention Management
```

---

## Report Types

```text
AUDIT_COMPLIANCE

SECURITY_COMPLIANCE

DATA_ACCESS_REPORT
```

---

## Methods

```java
generateComplianceReport()

getComplianceReports()
```

---

# 8. Audit Export Service

Responsibilities:

```text
Export Audit Data

Generate Files

Upload Files

Track Export Status
```

---

## Formats

```text
CSV

XLSX

PDF
```

---

## Storage

```text
AWS S3
```

---

# 9. Controller Layer

## AuditController

Base Path

```http
/api/v1/audit
```

---

## Endpoints

```http
GET  /audit/events

GET  /audit/logs

GET  /audit/security

GET  /audit/compliance

POST /audit/exports

GET  /audit/exports/{jobId}
```

**As-built (2026-07-29):** there is no `/audit/*` base path. The only audit read endpoint is **`GET /claims/{id}/audit`** (`AUDIT_READ`), which returns the `AuditLog` entries for that claim — tenant-scoped, so a claim from another tenant returns **404**. The security-event, compliance and export endpoints (and their workers, §14–§16) were not built.

---

# 10. DTO Design

## AuditEventResponse

```java
auditEventId

eventType

entityType

entityId

createdAt
```

---

## AuditLogResponse

```java
auditLogId

eventType

oldValue

newValue

createdBy

createdAt
```

---

## ComplianceReportResponse

```java
reportId

reportType

generatedAt
```

---

## AuditExportResponse

```java
jobId

status

downloadUrl
```

---

# 11. Repository Layer

## AuditEventRepository

Methods:

```java
findByEntityType()

findByEntityId()

findByCreatedAtBetween()
```

---

## AuditLogRepository

Methods:

```java
findByEventType()

findByCreatedBy()

findByTenantId()
```

---

## AuditExportRepository

Methods:

```java
findByStatus()

findByCreatedBy()
```

---

# 12. Audit Recording Strategy

## Standard Workflow

```text
Business Event
      ↓
Audit Consumer
      ↓
Create AuditEvent
      ↓
Create AuditLog
      ↓
Persist
```

---

## Example

```text
CLAIM_STATUS_CHANGED
```

Produces:

```text
AuditEvent

AuditLog
```

Containing:

```text
Old Status

New Status

Timestamp

User
```

---

# 13. Immutability Rules

Audit Records:

```text
Insert Only
```

---

Forbidden:

```text
Update

Delete
```

---

Implementation Rule:

Repositories must never expose:

```java
delete()

save(existingEntity)
```

for audit entities.

---

# 14. Compliance Report Workflow

```text
Generate Report
      ↓
Aggregate Audit Data
      ↓
Persist Report
      ↓
Publish Event
```

---

## Schedule

```text
Monthly

Quarterly

On Demand
```

---

# 15. Audit Export Workflow

```text
Export Requested
       ↓
Create Job
       ↓
Worker Generates File
       ↓
Upload To S3
       ↓
Generate Download URL
       ↓
Publish Event
```

---

# 16. Worker Design

## Audit Export Worker

Runs:

```text
Every 30 Seconds
```

---

Responsibilities:

```text
Process Pending Export Jobs

Generate Files

Upload To S3
```

---

# 17. Event Publishing

## AuditEventPublisher

Events:

```text
AUDIT_EVENT_CREATED

AUDIT_LOG_CREATED

USER_ACTIVITY_RECORDED

SECURITY_EVENT_RECORDED

COMPLIANCE_REPORT_GENERATED

AUDIT_EXPORT_REQUESTED

AUDIT_EXPORT_COMPLETED

AUDIT_EXPORT_FAILED
```

---

All events use:

```text
Outbox Pattern
```

---

# 18. Transaction Boundaries

## Record Audit Event

```java
@Transactional
```

Workflow:

```text
Persist Audit Event
       ↓
Persist Audit Log
       ↓
Save Event
       ↓
Commit
```

---

## Generate Compliance Report

```java
@Transactional
```

Workflow:

```text
Aggregate Data
       ↓
Persist Report
       ↓
Save Event
       ↓
Commit
```

---

# 19. Exception Handling

Exceptions:

```java
AuditEventNotFoundException

ComplianceReportException

AuditExportException

SecurityAuditException
```

---

# 20. Security Rules

Required Permissions:

```text
AUDIT_VIEW

AUDIT_EXPORT

COMPLIANCE_VIEW
```

**As-built (2026-07-29):** the only audit permission is **`AUDIT_READ`** (on `getClaimAudit`); there are no export/compliance permissions. Immutability (§13) holds in practice — `AuditService` only ever inserts. Tenant isolation is automatic via `@TenantId`.

---

Restricted Access:

```text
Audit Data Visible Only
To Authorized Roles
```

---

Tenant Isolation:

```text
All Queries Filtered
By tenant_id
```

---

# 21. Retention Strategy

Retention Period:

```text
7 Years
```

---

Applies To:

```text
Audit Events

Audit Logs

Security Events

Compliance Reports
```

---

# 22. Audit Integration

Every business module generates audit data:

```text
Claim

Document

Assignment

Investigation

Processing

Fraud

Notification

Analytics
```

---

The Audit Module consumes all operational events.

---

# 23. Performance Considerations

Indexes:

```text
entity_type

entity_id

event_type

tenant_id

created_at
```

---

Audit Search Target:

```text
< 1 Second
```

---

Export Generation Target:

```text
< 60 Seconds
```

---

# 24. Dependency Diagram

```text
Business Events
        ↓
Audit Consumer
        ↓
AuditService
        ↓
Audit Tables
        ↓
Audit APIs

AuditService
        ↓
ComplianceService
        ↓
Compliance Reports

AuditService
        ↓
AuditExportService
        ↓
AWS S3

AuditService
        ↓
AuditEventPublisher
        ↓
Outbox Table
```

---

# 25. Unit Testing Requirements

Coverage Target:

```text
90%+
```

Required Tests:

```text
Audit Event Recording

Audit Log Creation

Security Event Tracking

Compliance Report Generation

Export Generation

Immutability Validation
```

---

# 26. Future Enhancements

V2 Reserved:

```text
Regulatory Reporting

Data Access Auditing

Privileged User Monitoring

Compliance Violations

Audit Dashboards
```

---

# Approval

This document defines the Audit Module implementation blueprint and serves as the reference for immutable audit trails, compliance reporting, security monitoring, export processing, event publishing, transaction management, retention management, and future scalability.
