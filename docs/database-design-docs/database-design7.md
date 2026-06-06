# Database Design Part 13 - Analytics & Audit Domain

Status: Draft

Version: 1.0

Owner: RiskLens Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* analytics_snapshot
* dashboard_metric
* audit_event
* audit_log

This domain powers:

* Executive Dashboards
* Operational Analytics
* Compliance
* Regulatory Auditing
* User Activity Tracking
* System Traceability

This is one of the most important domains for enterprise adoption.

---

# Design Principles

Analytics and Auditing serve different purposes.

Analytics

```text
"What happened?"
```

Examples:

* Claims Submitted
* Claims Approved
* Fraud Detection Rate
* Investigator Productivity

---

Audit

```text
"Who did what and when?"
```

Examples:

* User Changed Claim Status
* Manager Approved Investigation
* Fraud Alert Resolved
* Assignment Reassigned

---

# Analytics Architecture

Operational Tables

↓

Aggregation Jobs

↓

Analytics Snapshot Tables

↓

Dashboard Queries

Purpose:

Avoid expensive dashboard queries on transactional tables.

---

# Table: analytics_snapshot

Purpose:

Stores aggregated metrics for reporting.

Schema:

analytics

---

Columns

| Column                         | Type          | Constraints |
| ------------------------------ | ------------- | ----------- |
| id                             | BIGSERIAL     | PRIMARY KEY |
| tenant_id                      | BIGINT        | NOT NULL    |
| snapshot_date                  | DATE          | NOT NULL    |
| total_claims                   | INTEGER       | DEFAULT 0   |
| submitted_claims               | INTEGER       | DEFAULT 0   |
| approved_claims                | INTEGER       | DEFAULT 0   |
| rejected_claims                | INTEGER       | DEFAULT 0   |
| open_claims                    | INTEGER       | DEFAULT 0   |
| fraud_flagged_claims           | INTEGER       | DEFAULT 0   |
| fraud_confirmed_claims         | INTEGER       | DEFAULT 0   |
| average_claim_processing_hours | NUMERIC(12,2) |             |
| average_investigation_hours    | NUMERIC(12,2) |             |
| created_at                     | TIMESTAMP     | NOT NULL    |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_analytics_snapshot
ON analytics.analytics_snapshot(
    tenant_id,
    snapshot_date
);
```

---

Indexes

```sql
CREATE INDEX idx_analytics_snapshot_date
ON analytics.analytics_snapshot(snapshot_date);

CREATE INDEX idx_analytics_snapshot_tenant
ON analytics.analytics_snapshot(tenant_id);
```

---

Purpose

Provides:

```text
Daily Analytics
```

without scanning millions of records.

---

# Table: dashboard_metric

Purpose:

Stores reusable KPI metrics.

Schema:

analytics

---

Columns

| Column        | Type          | Constraints |
| ------------- | ------------- | ----------- |
| id            | BIGSERIAL     | PRIMARY KEY |
| tenant_id     | BIGINT        | NOT NULL    |
| metric_code   | VARCHAR(100)  | NOT NULL    |
| metric_name   | VARCHAR(255)  | NOT NULL    |
| metric_value  | NUMERIC(18,2) |             |
| metric_unit   | VARCHAR(50)   |             |
| calculated_at | TIMESTAMP     | NOT NULL    |
| created_at    | TIMESTAMP     | NOT NULL    |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)
```

---

Examples

```text
TOTAL_OPEN_CLAIMS

AVG_INVESTIGATION_TIME

HIGH_RISK_CLAIMS

FRAUD_RATE

INVESTIGATOR_PRODUCTIVITY
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_dashboard_metric
ON analytics.dashboard_metric(
    tenant_id,
    metric_code
);
```

---

Indexes

```sql
CREATE INDEX idx_dashboard_metric_code
ON analytics.dashboard_metric(metric_code);

CREATE INDEX idx_dashboard_metric_tenant
ON analytics.dashboard_metric(tenant_id);
```

---

# Executive Dashboard Metrics

V1 Dashboard Supports

```text
Claims Submitted Today

Claims Pending Investigation

Claims Closed

Fraud Detection Rate

Average Investigation Time

Open Assignments

Investigator Workload

SLA Breaches
```

---

# Table: audit_event

Purpose:

Master catalog of auditable actions.

Schema:

audit

---

Columns

| Column      | Type         | Constraints |
| ----------- | ------------ | ----------- |
| id          | BIGSERIAL    | PRIMARY KEY |
| code        | VARCHAR(100) | NOT NULL    |
| name        | VARCHAR(255) | NOT NULL    |
| description | TEXT         |             |
| created_at  | TIMESTAMP    | NOT NULL    |

---

Examples

```text
CLAIM_CREATED

CLAIM_UPDATED

CLAIM_APPROVED

CLAIM_REJECTED

ASSIGNMENT_CREATED

ASSIGNMENT_ACCEPTED

INVESTIGATION_COMPLETED

FRAUD_ALERT_RESOLVED

USER_LOGIN

USER_LOGOUT
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_audit_event_code
ON audit.audit_event(code);
```

---

# Table: audit_log

Purpose:

Stores immutable audit records.

This is the compliance backbone of the platform.

Schema:

audit

---

Columns

| Column         | Type         | Constraints |
| -------------- | ------------ | ----------- |
| id             | BIGSERIAL    | PRIMARY KEY |
| tenant_id      | BIGINT       | NOT NULL    |
| audit_event_id | BIGINT       | NOT NULL    |
| user_id        | BIGINT       |             |
| entity_type    | VARCHAR(100) | NOT NULL    |
| entity_id      | BIGINT       |             |
| action         | VARCHAR(100) | NOT NULL    |
| previous_state | JSONB        |             |
| new_state      | JSONB        |             |
| ip_address     | VARCHAR(100) |             |
| user_agent     | TEXT         |             |
| correlation_id | VARCHAR(255) |             |
| created_at     | TIMESTAMP    | NOT NULL    |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)

audit_event_id
    → audit.audit_event(id)

user_id
    → user(id)
```

---

Examples

Entity Types

```text
CLAIM

ASSIGNMENT

INVESTIGATION

USER

DOCUMENT

FRAUD_ALERT
```

---

Action Examples

```text
CREATE

UPDATE

DELETE

APPROVE

REJECT

ASSIGN

UPLOAD

LOGIN
```

---

Indexes

```sql
CREATE INDEX idx_audit_log_tenant
ON audit.audit_log(tenant_id);

CREATE INDEX idx_audit_log_user
ON audit.audit_log(user_id);

CREATE INDEX idx_audit_log_entity
ON audit.audit_log(entity_type, entity_id);

CREATE INDEX idx_audit_log_created_at
ON audit.audit_log(created_at);

CREATE INDEX idx_audit_log_correlation
ON audit.audit_log(correlation_id);
```

---

# Correlation ID Strategy

Every request receives:

```text
correlation_id
```

Example

```text
REQ-9f8a4d2b
```

This allows tracing:

```text
API Request
    ↓
Claim Creation
    ↓
Document Upload
    ↓
OCR Job
    ↓
Fraud Analysis
```

across the entire system.

---

# Audit Retention Strategy

V1

```text
7 Years
```

Recommended for insurance compliance.

Audit records are:

```text
Immutable
```

Never updated.

Never deleted.

---

# Analytics Aggregation Strategy

Scheduled Job

↓

Daily Aggregation

↓

analytics_snapshot

↓

Dashboard Query

Benefits

```text
Fast Dashboards

Low Database Load

Historical Reporting
```

---

# Audit Domain ERD

AuditEvent

↓

AuditLog

---

User

↓

AuditLog

---

InsuranceCompany

↓

AuditLog

---

# Analytics Domain ERD

InsuranceCompany

↓

AnalyticsSnapshot

---

InsuranceCompany

↓

DashboardMetric

---

# Query Optimization Strategy

Common Analytics Queries

```sql
WHERE tenant_id = ?

WHERE snapshot_date >= ?

WHERE metric_code = ?
```

---

Common Audit Queries

```sql
WHERE user_id = ?

WHERE entity_type = ?

WHERE entity_id = ?

WHERE created_at >= ?
```

Indexes added accordingly.

---

# Future Enhancements

V2

```text
Real-Time Dashboards

Trend Analysis

Fraud Trend Reporting

Investigator Scorecards

Audit Export APIs

SIEM Integration
```

---

# Analytics & Audit Domain Summary

Tables

```text
analytics.analytics_snapshot

analytics.dashboard_metric

audit.audit_event

audit.audit_log
```

Supports

```text
Executive Dashboards            YES

Operational Reporting          YES

Compliance Auditing            YES

User Activity Tracking         YES

Entity Change Tracking         YES

Historical Reporting           YES

Request Traceability           YES

Regulatory Readiness           YES
```

Core Analytics Entity

```text
analytics_snapshot
```

Core Audit Entity

```text
audit_log
```

Enterprise Compliance Ready

```text
YES
```
