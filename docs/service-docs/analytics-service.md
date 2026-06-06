# 09.9 Analytics Service Design

## Document Information

| Field           | Value                    |
| --------------- | ------------------------ |
| Project         | ClaimLens                |
| Company         | RiskLens Technologies    |
| Version         | V1                       |
| Document Type   | Service Design           |
| Document Number | 09.9                     |
| Document Name   | Analytics Service Design |
| Status          | Approved                 |
| Last Updated    | June 2026                |

---

# 1. Overview

The Analytics Module is responsible for transforming operational events into dashboards, KPIs, reports, and historical insights.

Responsibilities:

* KPI Aggregation
* Dashboard Metrics
* Analytics Snapshots
* Trend Analysis
* Report Generation
* Report Export
* Executive Reporting

The Analytics Module is a read-only consumer of business events.

---

# 2. Module Dependencies

## Consumed Events

```text
CLAIM_CREATED

CLAIM_CLOSED

ASSIGNMENT_CREATED

ASSIGNMENT_COMPLETED

INVESTIGATION_COMPLETED

FRAUD_ALERT_CREATED

FRAUD_ALERT_CLOSED

OCR_COMPLETED

ANALYSIS_COMPLETED

NOTIFICATION_DELIVERED
```

---

## Published Events

```text
ANALYTICS_SNAPSHOT_STARTED

ANALYTICS_SNAPSHOT_CREATED

DASHBOARD_METRICS_UPDATED

KPI_CALCULATED

REPORT_EXPORT_REQUESTED

REPORT_EXPORT_COMPLETED

REPORT_EXPORT_FAILED
```

---

# 3. Package Structure

```text
analytics

├── controller
│   └── AnalyticsController

├── service
│   ├── AnalyticsService
│   ├── DashboardService
│   ├── KpiService
│   ├── SnapshotService
│   ├── ReportExportService
│   └── AnalyticsServiceImpl

├── repository
│   ├── AnalyticsSnapshotRepository
│   ├── DashboardMetricRepository
│   └── ReportExportRepository

├── entity
│   ├── AnalyticsSnapshot
│   ├── DashboardMetric
│   └── ReportExportJob

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
AnalyticsSnapshot

DashboardMetric

ReportExportJob
```

---

# 5. Analytics Service

Responsibilities:

```text
Analytics Queries

Trend Analysis

Dashboard Data Retrieval
```

---

## Methods

```java
getDashboardMetrics()

getFraudAnalytics()

getInvestigationAnalytics()

getOperationalMetrics()
```

---

# 6. KPI Service

Responsibilities:

```text
KPI Calculation

Metric Aggregation

Historical KPI Tracking
```

---

## Example KPIs

```text
TOTAL_CLAIMS

ACTIVE_CLAIMS

HIGH_RISK_CLAIMS

AVERAGE_FRAUD_SCORE

SLA_COMPLIANCE_RATE

AVG_INVESTIGATION_DURATION
```

---

## Methods

```java
calculateKpis()

calculateMetric()

getMetricHistory()
```

---

# 7. Dashboard Service

Responsibilities:

```text
Dashboard Aggregation

Dashboard Read Models

Performance Optimized Queries
```

---

## Dashboard Sections

### Executive Dashboard

```text
Total Claims

Open Claims

Fraud Alerts

Investigation Backlog
```

---

### Operations Dashboard

```text
Processing Queue

OCR Throughput

Analysis Throughput

SLA Violations
```

---

### Fraud Dashboard

```text
Fraud Score Distribution

Fraud Alert Trends

Top Triggered Rules
```

---

# 8. Snapshot Service

Purpose:

Generate immutable analytics snapshots.

---

## Snapshot Types

```text
DAILY

WEEKLY

MONTHLY
```

---

## Methods

```java
createSnapshot()

generateDailySnapshot()

generateMonthlySnapshot()
```

---

# 9. Report Export Service

Responsibilities:

```text
Export Reports

Generate Files

Store Exports

Track Status
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

## Methods

```java
requestExport()

generateExport()

downloadExport()
```

---

# 10. Controller Layer

## AnalyticsController

Base Path

```http
/api/v1/analytics
```

---

## Endpoints

```http
GET  /analytics/dashboard

GET  /analytics/fraud

GET  /analytics/investigations

GET  /analytics/operations

GET  /analytics/kpis

POST /analytics/exports

GET  /analytics/exports/{jobId}
```

---

# 11. DTO Design

## DashboardResponse

```java
totalClaims

activeClaims

fraudAlerts

openInvestigations
```

---

## KpiResponse

```java
metricCode

metricValue

calculatedAt
```

---

## ExportRequest

```java
reportType

format

dateFrom

dateTo
```

---

## ExportResponse

```java
jobId

status

downloadUrl
```

---

# 12. Repository Layer

## DashboardMetricRepository

Methods:

```java
findByMetricCode()

findBySnapshotDate()
```

---

## AnalyticsSnapshotRepository

Methods:

```java
findLatestSnapshot()

findBySnapshotType()
```

---

## ReportExportRepository

Methods:

```java
findByStatus()

findByCreatedBy()
```

---

# 13. Analytics Aggregation Strategy

## Event Driven Updates

```text
Business Event
      ↓
Analytics Consumer
      ↓
Dashboard Metrics Updated
```

---

## Example

```text
CLAIM_CREATED
      ↓
TOTAL_CLAIMS +1
```

---

# 14. Snapshot Generation Workflow

```text
Scheduler
      ↓
Snapshot Service
      ↓
Aggregate Metrics
      ↓
Persist Snapshot
      ↓
Publish Event
```

---

## Schedule

```text
Daily 01:00 AM

Weekly Sunday 02:00 AM

Monthly 03:00 AM
```

---

# 15. Report Export Workflow

```text
User Requests Export
         ↓
Create Export Job
         ↓
Worker Generates File
         ↓
Upload To S3
         ↓
Generate Download URL
         ↓
Notify User
```

---

# 16. Worker Design

## Snapshot Worker

Runs:

```text
Scheduled
```

Purpose:

```text
Generate Snapshots
```

---

## Export Worker

Runs:

```text
Every 30 Seconds
```

Purpose:

```text
Generate Pending Reports
```

---

# 17. Event Publishing

## AnalyticsEventPublisher

Events:

```text
ANALYTICS_SNAPSHOT_STARTED

ANALYTICS_SNAPSHOT_CREATED

DASHBOARD_METRICS_UPDATED

KPI_CALCULATED

REPORT_EXPORT_REQUESTED

REPORT_EXPORT_COMPLETED

REPORT_EXPORT_FAILED
```

---

All events use:

```text
Outbox Pattern
```

---

# 18. Transaction Boundaries

## Generate Snapshot

```java
@Transactional
```

Workflow:

```text
Aggregate Data
      ↓
Persist Snapshot
      ↓
Persist Metrics
      ↓
Save Event
      ↓
Commit
```

---

## Request Export

```java
@Transactional
```

Workflow:

```text
Validate Request
      ↓
Create Export Job
      ↓
Save Event
      ↓
Commit
```

---

# 19. Exception Handling

Exceptions:

```java
AnalyticsSnapshotException

ExportGenerationException

MetricCalculationException

ReportNotFoundException
```

---

# 20. Security Rules

Required Permissions:

```text
ANALYTICS_VIEW

ANALYTICS_EXPORT
```

---

Tenant Isolation:

```text
All Queries Filtered
By tenant_id
```

---

# 21. Audit Integration

Tracked:

```text
Snapshot Generated

Export Requested

Export Generated

Dashboard Viewed
```

---

Every write operation generates:

```text
AUDIT_EVENT_CREATED
```

---

# 22. Performance Considerations

Indexes:

```text
metric_code

snapshot_date

snapshot_type

tenant_id
```

---

Dashboard Target:

```text
< 500ms
```

---

Export Generation Target:

```text
< 60 Seconds
```

---

# 23. Dependency Diagram

```text
Business Events
        ↓
Analytics Consumer
        ↓
KpiService
        ↓
DashboardService
        ↓
Analytics Tables
        ↓
Dashboard APIs

AnalyticsService
        ↓
ReportExportService
        ↓
AWS S3

AnalyticsService
        ↓
AnalyticsEventPublisher
        ↓
Outbox Table
```

---

# 24. Unit Testing Requirements

Coverage Target:

```text
90%+
```

Required Tests:

```text
KPI Calculation

Dashboard Aggregation

Snapshot Generation

Export Creation

Export Completion

Metric Updates
```

---

# 25. Future Enhancements

V2 Reserved:

```text
Real-Time Dashboards

Predictive Analytics

Fraud Forecasting

ML Insights

Anomaly Detection
```

---

# Approval

This document defines the Analytics Module implementation blueprint and serves as the reference for KPI generation, dashboard aggregation, analytics snapshots, report exports, event publishing, transaction management, audit integration, and future scalability.
