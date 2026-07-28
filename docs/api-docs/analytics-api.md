> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; where it diverges from the shipped code the authoritative sources win: [`../domain-model.md`](../domain-model.md), [`../architecture.md`](../architecture.md), [`../audit-report.md`](../audit-report.md), and the running API. Concrete divergences are flagged inline as **As-built** notes.

# 07.14 Analytics API

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | API Design            |
| Document Number | 07.14                 |
| Document Name   | Analytics API         |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

The Analytics Module is the reporting and business intelligence layer of ClaimLens.

Its primary responsibility is to transform operational data into analytical insights.

The Analytics Module powers:

* Executive Dashboards
* Operational Dashboards
* Fraud Analytics
* Investigator Performance Analytics
* SLA Monitoring
* Claim Trends
* Branch Analytics
* Region Analytics
* KPI Reporting
* Historical Reporting
* Data Exports

Analytics data is generated through scheduled aggregation jobs and stored separately from transactional tables.

---

# 2. Domain Entities

Managed Entities

```text
analytics_snapshot
dashboard_metric
```

Related Entities

```text
claim
assignment
investigation
fraud_score
fraud_alert
notification
ocr_job
analysis_job
```

---

# 3. Analytics Architecture

## Analytics Flow

```text
Transactional Data
        ↓
Aggregation Jobs
        ↓
analytics_snapshot
dashboard_metric
        ↓
Analytics APIs
        ↓
Dashboards & Reports
```

---

## Aggregation Strategy

Analytics data is not calculated on every request.

Instead:

```text
Scheduled Analytics Worker
```

creates snapshots periodically.

Benefits:

* Faster dashboards
* Lower database load
* Historical reporting
* Trend analysis

---

# 4. Permission Matrix

| Permission       | Description           |
| ---------------- | --------------------- |
| ANALYTICS_VIEW   | View analytics        |
| ANALYTICS_EXPORT | Export reports        |
| ANALYTICS_ADMIN  | Manage analytics jobs |
| KPI_VIEW         | View KPI metrics      |

**As-built (2026-07-29):** The analytics module ships exactly ONE endpoint — `GET /api/v1/analytics/dashboard` (permission `ANALYTICS_READ`), which computes an aggregated dashboard object on-request. There is no `analytics_snapshot`/`dashboard_metric` table and no scheduled aggregation worker. None of the snapshot, dashboard-metric, claims/fraud/investigation/investigator/SLA/branch/region/processing, export, or aggregation-admin endpoints in the sections below were built. The permission codes `ANALYTICS_VIEW`, `ANALYTICS_EXPORT`, `ANALYTICS_ADMIN`, and `KPI_VIEW` do not exist; the only analytics permission is `ANALYTICS_READ`.

---

# 5. Analytics Snapshot APIs

**As-built (2026-07-29):** Not built — see the module-wide note in §4. Sections 5 through 16 describe a design-era analytics surface that was never implemented; the shipped surface is the single `GET /api/v1/analytics/dashboard` endpoint.

---

## Get Analytics Snapshot

### Endpoint

```http
GET /api/v1/analytics/snapshots/{snapshotId}
```

### Success Response

```json
{
  "snapshotId": 1001,
  "snapshotDate": "2026-06-01",
  "snapshotType": "DAILY"
}
```

---

## List Analytics Snapshots

### Endpoint

```http
GET /api/v1/analytics/snapshots
```

### Query Parameters

```text
snapshotType
fromDate
toDate
page
size
```

### Success Response

```json
{
  "content": [],
  "totalElements": 0
}
```

---

# 6. Dashboard Metric APIs

---

## Get Dashboard Metrics

### Endpoint

```http
GET /api/v1/analytics/dashboard-metrics
```

### Query Parameters

```text
metricCode
fromDate
toDate
```

### Success Response

```json
[
  {
    "metricCode": "TOTAL_CLAIMS",
    "metricValue": 15000
  }
]
```

---

## Get Dashboard Metric

### Endpoint

```http
GET /api/v1/analytics/dashboard-metrics/{metricCode}
```

### Success Response

```json
{
  "metricCode": "TOTAL_CLAIMS",
  "metricValue": 15000
}
```

---

# 7. Claim Analytics APIs

---

## Claims Summary Analytics

### Endpoint

```http
GET /api/v1/analytics/claims/summary
```

### Success Response

```json
{
  "totalClaims": 15000,
  "approvedClaims": 12500,
  "rejectedClaims": 1880,
  "activeClaims": 620
}
```

---

## Claims Trend Analytics

### Endpoint

```http
GET /api/v1/analytics/claims/trends
```

### Query Parameters

```text
fromDate
toDate
interval
```

### Success Response

```json
{
  "trendData": []
}
```

---

## Claims By Status

### Endpoint

```http
GET /api/v1/analytics/claims/status-distribution
```

### Success Response

```json
[
  {
    "status": "UNDER_REVIEW",
    "count": 125
  }
]
```

---

# 8. Fraud Analytics APIs

---

## Fraud Summary Analytics

### Endpoint

```http
GET /api/v1/analytics/fraud/summary
```

### Success Response

```json
{
  "evaluatedClaims": 14500,
  "highRiskClaims": 520,
  "criticalRiskClaims": 110,
  "averageFraudScore": 38.7
}
```

---

## Fraud Trend Analytics

### Endpoint

```http
GET /api/v1/analytics/fraud/trends
```

### Success Response

```json
{
  "dailyFraudCases": []
}
```

---

## Fraud Rule Effectiveness

### Endpoint

```http
GET /api/v1/analytics/fraud/rule-effectiveness
```

### Success Response

```json
[
  {
    "ruleCode": "IMAGE_TAMPERING",
    "triggerCount": 350,
    "confirmedFraudCases": 120
  }
]
```

---

# 9. Investigation Analytics APIs

---

## Investigation Summary

### Endpoint

```http
GET /api/v1/analytics/investigations/summary
```

### Success Response

```json
{
  "activeInvestigations": 250,
  "completedInvestigations": 5200,
  "averageResolutionDays": 4.8
}
```

---

## Investigation Trends

### Endpoint

```http
GET /api/v1/analytics/investigations/trends
```

### Success Response

```json
{
  "dailyInvestigations": []
}
```

---

# 10. Investigator Performance Analytics

---

## Investigator Performance Report

### Endpoint

```http
GET /api/v1/analytics/investigators/performance
```

### Query Parameters

```text
regionId
branchId
```

### Success Response

```json
[
  {
    "investigatorId": 1001,
    "completedInvestigations": 180,
    "averageResolutionDays": 3.9,
    "fraudCasesDetected": 25
  }
]
```

---

## Investigator Leaderboard

### Endpoint

```http
GET /api/v1/analytics/investigators/leaderboard
```

### Success Response

```json
[
  {
    "rank": 1,
    "investigatorId": 1001,
    "score": 98.2
  }
]
```

---

# 11. SLA Analytics APIs

---

## SLA Compliance Summary

### Endpoint

```http
GET /api/v1/analytics/sla/summary
```

### Success Response

```json
{
  "slaComplianceRate": 96.8,
  "slaBreaches": 52
}
```

---

## SLA Breach Report

### Endpoint

```http
GET /api/v1/analytics/sla/breaches
```

### Success Response

```json
[
  {
    "claimId": 10001,
    "breachHours": 12
  }
]
```

---

# 12. Branch Analytics APIs

---

## Branch Performance

### Endpoint

```http
GET /api/v1/analytics/branches/performance
```

### Success Response

```json
[
  {
    "branchId": 10,
    "claimsProcessed": 2500,
    "fraudCases": 110
  }
]
```

---

## Branch Comparison

### Endpoint

```http
GET /api/v1/analytics/branches/comparison
```

### Success Response

```json
{
  "branches": []
}
```

---

# 13. Region Analytics APIs

---

## Region Performance

### Endpoint

```http
GET /api/v1/analytics/regions/performance
```

### Success Response

```json
[
  {
    "regionId": 1,
    "claimsProcessed": 7500,
    "fraudCases": 240
  }
]
```

---

## Region Comparison

### Endpoint

```http
GET /api/v1/analytics/regions/comparison
```

### Success Response

```json
{
  "regions": []
}
```

---

# 14. Processing Analytics APIs

---

## OCR Analytics

### Endpoint

```http
GET /api/v1/analytics/processing/ocr
```

### Success Response

```json
{
  "totalJobs": 15000,
  "successfulJobs": 14880,
  "failedJobs": 120,
  "averageProcessingTimeMs": 3200
}
```

---

## Analysis Analytics

### Endpoint

```http
GET /api/v1/analytics/processing/analysis
```

### Success Response

```json
{
  "totalJobs": 14800,
  "successfulJobs": 14720,
  "failedJobs": 80
}
```

---

# 15. Export APIs

---

## Export Analytics Report

### Endpoint

```http
POST /api/v1/analytics/export
```

### Permissions

```text
ANALYTICS_EXPORT
```

### Request Body

```json
{
  "reportType": "FRAUD_ANALYTICS",
  "format": "XLSX",
  "fromDate": "2026-06-01",
  "toDate": "2026-06-30"
}
```

### Success Response

```json
{
  "exportJobId": 5001
}
```

### Audit Event

```text
ANALYTICS_EXPORT_REQUESTED
```

---

## Get Export Status

### Endpoint

```http
GET /api/v1/analytics/export/{exportJobId}
```

### Success Response

```json
{
  "status": "COMPLETED",
  "downloadUrl": "https://presigned-url"
}
```

---

# 16. Analytics Administration APIs

---

## Trigger Analytics Aggregation

### Endpoint

```http
POST /api/v1/analytics/aggregation/run
```

### Permissions

```text
ANALYTICS_ADMIN
```

### Success Response

```json
{
  "message": "Aggregation job started"
}
```

### Audit Event

```text
ANALYTICS_AGGREGATION_TRIGGERED
```

---

## Get Aggregation Status

### Endpoint

```http
GET /api/v1/analytics/aggregation/status
```

### Success Response

```json
{
  "running": false,
  "lastRunAt": "2026-06-01T23:59:00Z"
}
```

---

# 17. Business Validation Rules

## Analytics Queries

* Tenant isolation mandatory.
* Date range validation required.
* Historical data immutable.

## Exports

* Export size limits configurable.
* Export links expire after 24 hours.

## Aggregation

* Only one aggregation job may run at a time.
* Aggregation results stored in analytics_snapshot.

---

# 18. Error Codes

| Error Code                  | Description        |
| --------------------------- | ------------------ |
| SNAPSHOT_NOT_FOUND          | Snapshot missing   |
| METRIC_NOT_FOUND            | Metric missing     |
| INVALID_DATE_RANGE          | Invalid range      |
| EXPORT_JOB_NOT_FOUND        | Export missing     |
| AGGREGATION_ALREADY_RUNNING | Aggregation active |
| ANALYTICS_ACCESS_DENIED     | Access denied      |

---

# 19. Audit Events

```text
ANALYTICS_VIEWED
KPI_VIEWED

ANALYTICS_EXPORT_REQUESTED

ANALYTICS_AGGREGATION_TRIGGERED
```

---

# 20. Integration Events

Consumed From:

```text
CLAIM MODULE
DOCUMENT MODULE
ASSIGNMENT MODULE
INVESTIGATION MODULE
PROCESSING MODULE
FRAUD MODULE
NOTIFICATION MODULE
```

Produces:

```text
ANALYTICS_SNAPSHOT_CREATED
DASHBOARD_METRICS_UPDATED
```

---

# 21. Performance Requirements

| API                | Target   |
| ------------------ | -------- |
| Summary Analytics  | < 300 ms |
| Trend Analytics    | < 500 ms |
| KPI Retrieval      | < 200 ms |
| Export Request     | < 500 ms |
| Snapshot Retrieval | < 200 ms |

---

# 22. Security Requirements

All APIs require:

```text
JWT Authentication
Tenant Validation
Permission Validation
Audit Logging
```

Additional Controls:

* Executive analytics restricted to leadership roles.
* Fraud analytics restricted to fraud analysts.
* Exports logged and auditable.
* Cross-tenant analytics prohibited.

---

# Approval

This document defines the Analytics API contract for ClaimLens and serves as the implementation reference for KPI reporting, trend analysis, fraud analytics, SLA analytics, investigator performance reporting, branch analytics, regional analytics, dashboard data aggregation, and executive business intelligence.
