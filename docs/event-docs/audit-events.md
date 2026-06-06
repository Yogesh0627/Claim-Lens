# 08.10 Audit Events

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | RiskLens Technologies |
| Version         | V1                    |
| Document Type   | Event Design          |
| Document Number | 08.10                 |
| Document Name   | Audit Events          |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

This document defines all events produced by the Audit Module.

The Audit Module provides:

* Immutable Audit Trails
* User Activity Tracking
* Security Event Tracking
* Compliance Reporting
* Regulatory Reporting
* Audit Exports
* Historical Change Tracking

The Audit Module consumes events from every module within ClaimLens.

---

# 2. Audit Event Catalog

| Event                       | Category    |
| --------------------------- | ----------- |
| AUDIT_EVENT_CREATED         | Integration |
| AUDIT_LOG_CREATED           | Integration |
| USER_ACTIVITY_RECORDED      | Domain      |
| SECURITY_EVENT_RECORDED     | Domain      |
| COMPLIANCE_REPORT_GENERATED | Integration |
| AUDIT_EXPORT_REQUESTED      | Integration |
| AUDIT_EXPORT_COMPLETED      | Integration |
| AUDIT_EXPORT_FAILED         | System      |

---

# 3. AUDIT_EVENT_CREATED

## Category

```text id="z77h7x"
Integration Event
```

---

## Trigger

Business event successfully recorded by Audit Module.

---

## Producer

```text id="z8d6m0"
Audit Module
```

---

## Consumers

```text id="m9h8r4"
Compliance Module
Analytics Module
```

---

## Source Events

```text id="t4v6c2"
CLAIM_CREATED

DOCUMENT_UPLOADED

ASSIGNMENT_CREATED

INVESTIGATION_COMPLETED

FRAUD_ALERT_CREATED

NOTIFICATION_DELIVERED
```

---

## Payload

```json id="d9s3x7"
{
  "auditEventId": 1001,
  "eventType": "CLAIM_CREATED",
  "entityType": "CLAIM",
  "entityId": 10001
}
```

---

# 4. AUDIT_LOG_CREATED

## Category

```text id="v7g1q5"
Integration Event
```

---

## Trigger

Detailed audit log persisted.

---

## Producer

```text id="g3y6f9"
Audit Module
```

---

## Consumers

```text id="f2r9m4"
Compliance Module
Analytics Module
```

---

## Payload

```json id="w4p8s2"
{
  "auditLogId": 5001,
  "auditEventId": 1001
}
```

---

## Example

Tracked Changes:

```text id="a8q5k7"
Old Status: SUBMITTED

New Status: UNDER_REVIEW
```

---

# 5. USER_ACTIVITY_RECORDED

## Category

```text id="c6j3n9"
Domain Event
```

---

## Trigger

User activity captured.

---

## Producer

```text id="q8d4w2"
Audit Module
```

---

## Consumers

```text id="e7f1r5"
Analytics Module
Compliance Module
```

---

## Payload

```json id="u9k2v8"
{
  "userId": 101,
  "activityType": "CLAIM_UPDATED",
  "entityId": 10001
}
```

---

## Tracked Activities

```text id="y4t8m6"
LOGIN

LOGOUT

CLAIM_UPDATED

DOCUMENT_UPLOADED

REPORT_EXPORTED
```

---

# 6. SECURITY_EVENT_RECORDED

## Category

```text id="n2w5x7"
Domain Event
```

---

## Trigger

Security-sensitive action detected.

---

## Producer

```text id="j5r8c1"
Audit Module
```

---

## Consumers

```text id="l4v9p2"
Compliance Module
Analytics Module
Monitoring Module
```

---

## Payload

```json id="m6q3s8"
{
  "securityEventId": 9001,
  "eventType": "LOGIN_FAILED",
  "severity": "HIGH"
}
```

---

## Security Events

```text id="t7n4f3"
LOGIN_FAILED

PERMISSION_DENIED

ROLE_CHANGED

MULTIPLE_FAILED_LOGINS

ACCOUNT_LOCKED
```

---

# 7. COMPLIANCE_REPORT_GENERATED

## Category

```text id="r3y7d2"
Integration Event
```

---

## Trigger

Compliance report successfully generated.

---

## Producer

```text id="s8m1q4"
Audit Module
```

---

## Consumers

```text id="h5v6r2"
Notification Module
Analytics Module
```

---

## Payload

```json id="k1x9p6"
{
  "reportId": 7001,
  "reportType": "AUDIT_COMPLIANCE"
}
```

---

# 8. AUDIT_EXPORT_REQUESTED

## Category

```text id="b7f4k9"
Integration Event
```

---

## Trigger

User requests audit export.

---

## Producer

```text id="n9w3c5"
Audit Module
```

---

## Consumers

```text id="q2s6j8"
Audit Export Worker
```

---

## Payload

```json id="p4m8x1"
{
  "exportJobId": 5001,
  "format": "XLSX"
}
```

---

## Supported Formats

```text id="g6v2d9"
CSV

XLSX

PDF
```

---

# 9. AUDIT_EXPORT_COMPLETED

## Category

```text id="t1k5p7"
Integration Event
```

---

## Trigger

Audit export completed.

---

## Producer

```text id="r8v3n6"
Audit Export Worker
```

---

## Consumers

```text id="d4q7m2"
Notification Module
Analytics Module
```

---

## Payload

```json id="z6s8w4"
{
  "exportJobId": 5001,
  "fileLocation": "s3://audit-exports/report.xlsx"
}
```

---

# 10. AUDIT_EXPORT_FAILED

## Category

```text id="j9c2f5"
System Event
```

---

## Trigger

Audit export generation failed.

---

## Producer

```text id="x4p7v8"
Audit Export Worker
```

---

## Consumers

```text id="u3w6k1"
Notification Module
Monitoring Module
```

---

## Payload

```json id="y8n5r3"
{
  "exportJobId": 5001,
  "reason": "FILE_GENERATION_FAILED"
}
```

---

# 11. Audit Workflow

## Standard Audit Flow

```text id="o4x7j9"
CLAIM_CREATED
       ↓
AUDIT_EVENT_CREATED
       ↓
AUDIT_LOG_CREATED
```

---

## Security Audit Flow

```text id="v6m2s8"
LOGIN_FAILED
       ↓
SECURITY_EVENT_RECORDED
```

---

## Export Workflow

```text id="n1r8p5"
AUDIT_EXPORT_REQUESTED
          ↓
AUDIT_EXPORT_COMPLETED
```

---

# 12. Audit Sources

The Audit Module consumes events from:

```text id="q5v3m9"
Auth Module

Organization Module

User Module

Claim Module

Document Module

Assignment Module

Investigation Module

Processing Module

Fraud Module

Notification Module

Analytics Module
```

---

# 13. Retention Rules

Audit Records:

```text id="w8j4r6"
7 Years
```

---

## Security Events

```text id="m3v9p1"
7 Years
```

---

## Compliance Reports

```text id="s7x5n2"
7 Years
```

---

# 14. Immutability Rules

Audit records:

```text id="z2r6k8"
Cannot Be Updated

Cannot Be Deleted

Cannot Be Overwritten
```

---

# 15. Business Validation Rules

## Audit Records

* Event must exist.
* Timestamp mandatory.
* Tenant required.

## Security Events

* Severity required.
* User context required.

## Exports

* Authorization required.
* Expiring download links.

---

# 16. Monitoring Metrics

```text id="v9p2m4"
audit_events_created_total

audit_logs_created_total

security_events_recorded_total

compliance_reports_generated_total

audit_export_completed_total
```

---

# 17. Security Requirements

Events must not contain:

```text id="r6k1v8"
Passwords

JWT Tokens

API Keys

Secrets

Sensitive PII
```

Only identifiers and metadata.

---

# 18. Future Events (V2)

Reserved:

```text id="n8s3w6"
REGULATORY_REPORT_GENERATED

DATA_ACCESS_AUDITED

PRIVILEGED_ACTION_RECORDED

COMPLIANCE_VIOLATION_DETECTED
```

---

# Approval

This document defines the Audit Event Catalog for ClaimLens and serves as the implementation reference for immutable audit trails, user activity tracking, security monitoring, compliance reporting, export processing, analytics aggregation, and regulatory support.
