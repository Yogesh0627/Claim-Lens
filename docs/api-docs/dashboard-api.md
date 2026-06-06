# 07.13 Dashboard & KPI API

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | RiskLens Technologies |
| Version         | V1                    |
| Document Type   | API Design            |
| Document Number | 07.13                 |
| Document Name   | Dashboard & KPI API   |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

The Dashboard Module provides real-time and aggregated business intelligence across the ClaimLens platform.

It serves multiple personas:

* Executives
* Claims Managers
* Investigation Managers
* Investigators
* Fraud Analysts
* Operations Teams

The Dashboard APIs aggregate data from:

```text
claims
assignments
investigations
fraud
notifications
processing
analytics
```

and provide KPI-driven views optimized for operational decision making.

---

# 2. Dashboard Architecture

## Data Sources

```text
PostgreSQL
     ↓
Analytics Aggregator
     ↓
analytics_snapshot
dashboard_metric
     ↓
Dashboard APIs
     ↓
Frontend Widgets
```

---

## Dashboard Types

```text
EXECUTIVE
CLAIMS
INVESTIGATION
FRAUD
OPERATIONS
INVESTIGATOR
```

---

# 3. Permission Matrix

| Permission                | Description               |
| ------------------------- | ------------------------- |
| DASHBOARD_VIEW            | View dashboards           |
| EXECUTIVE_DASHBOARD_VIEW  | View executive dashboard  |
| FRAUD_DASHBOARD_VIEW      | View fraud dashboard      |
| OPERATIONS_DASHBOARD_VIEW | View operations dashboard |
| KPI_EXPORT                | Export dashboard data     |

---

# 4. Global Dashboard Filters

Supported Across All Dashboards

### Query Parameters

```text
fromDate
toDate
claimTypeId
regionId
branchId
investigatorId
riskLevel
```

Example:

```http
GET /api/v1/dashboard/executive?fromDate=2026-06-01&toDate=2026-06-30
```

---

# 5. Executive Dashboard APIs

Executive dashboard provides a business-wide overview.

---

## Executive Dashboard Summary

### Endpoint

```http
GET /api/v1/dashboard/executive
```

### Permissions

```text
EXECUTIVE_DASHBOARD_VIEW
```

### Success Response

```json
{
  "totalClaims": 15000,
  "activeClaims": 1200,
  "fraudDetectedClaims": 620,
  "claimsApproved": 12500,
  "claimsRejected": 1880,
  "averageFraudScore": 36.5
}
```

---

## Executive KPI Cards

### Endpoint

```http
GET /api/v1/dashboard/executive/kpis
```

### Success Response

```json
[
  {
    "metric": "TOTAL_CLAIMS",
    "value": 15000
  },
  {
    "metric": "FRAUD_DETECTION_RATE",
    "value": 4.1
  }
]
```

---

## Executive Trend Analysis

### Endpoint

```http
GET /api/v1/dashboard/executive/trends
```

### Success Response

```json
{
  "dailyClaims": [],
  "dailyFraudCases": [],
  "dailyInvestigations": []
}
```

---

# 6. Claims Dashboard APIs

---

## Claims Dashboard Summary

### Endpoint

```http
GET /api/v1/dashboard/claims
```

### Success Response

```json
{
  "submittedClaims": 500,
  "underReviewClaims": 120,
  "pendingInformationClaims": 30,
  "approvedClaims": 280,
  "rejectedClaims": 70
}
```

---

## Claims By Status

### Endpoint

```http
GET /api/v1/dashboard/claims/status-distribution
```

### Success Response

```json
[
  {
    "status": "UNDER_REVIEW",
    "count": 120
  }
]
```

---

## Claims By Region

### Endpoint

```http
GET /api/v1/dashboard/claims/regions
```

### Success Response

```json
[
  {
    "region": "North",
    "claims": 350
  }
]
```

---

## Claims Processing Trends

### Endpoint

```http
GET /api/v1/dashboard/claims/trends
```

### Success Response

```json
{
  "dailySubmissions": [],
  "dailyApprovals": [],
  "dailyRejections": []
}
```

---

# 7. Investigation Dashboard APIs

---

## Investigation Dashboard Summary

### Endpoint

```http
GET /api/v1/dashboard/investigations
```

### Success Response

```json
{
  "activeInvestigations": 240,
  "completedInvestigations": 5200,
  "averageCompletionDays": 4.8
}
```

---

## Investigation Status Distribution

### Endpoint

```http
GET /api/v1/dashboard/investigations/status-distribution
```

### Success Response

```json
[
  {
    "status": "IN_PROGRESS",
    "count": 120
  }
]
```

---

## Investigator Performance

### Endpoint

```http
GET /api/v1/dashboard/investigations/investigator-performance
```

### Success Response

```json
[
  {
    "investigatorId": 1001,
    "completedInvestigations": 180,
    "averageResolutionDays": 3.9
  }
]
```

---

# 8. Fraud Dashboard APIs

---

## Fraud Dashboard Summary

### Endpoint

```http
GET /api/v1/dashboard/fraud
```

### Permissions

```text
FRAUD_DASHBOARD_VIEW
```

### Success Response

```json
{
  "highRiskClaims": 420,
  "criticalRiskClaims": 95,
  "fraudAlertsOpen": 180,
  "averageFraudScore": 42.6
}
```

---

## Fraud Risk Distribution

### Endpoint

```http
GET /api/v1/dashboard/fraud/risk-distribution
```

### Success Response

```json
[
  {
    "riskLevel": "HIGH",
    "count": 420
  }
]
```

---

## Top Triggered Fraud Rules

### Endpoint

```http
GET /api/v1/dashboard/fraud/top-rules
```

### Success Response

```json
[
  {
    "ruleCode": "IMAGE_TAMPERING",
    "triggerCount": 350
  }
]
```

---

## Fraud Trend Analysis

### Endpoint

```http
GET /api/v1/dashboard/fraud/trends
```

### Success Response

```json
{
  "dailyFraudAlerts": [],
  "dailyHighRiskClaims": []
}
```

---

# 9. Operations Dashboard APIs

---

## Operations Dashboard Summary

### Endpoint

```http
GET /api/v1/dashboard/operations
```

### Permissions

```text
OPERATIONS_DASHBOARD_VIEW
```

### Success Response

```json
{
  "pendingAssignments": 18,
  "queuedProcessingJobs": 12,
  "failedProcessingJobs": 4,
  "notificationFailures": 7
}
```

---

## Processing Health Metrics

### Endpoint

```http
GET /api/v1/dashboard/operations/processing-health
```

### Success Response

```json
{
  "ocrJobsPending": 5,
  "analysisJobsPending": 7,
  "failedJobs": 4
}
```

---

## Queue Statistics

### Endpoint

```http
GET /api/v1/dashboard/operations/queue-statistics
```

### Success Response

```json
{
  "assignmentQueue": 18,
  "processingQueue": 12
}
```

---

# 10. Investigator Dashboard APIs

---

## Investigator Dashboard

### Endpoint

```http
GET /api/v1/dashboard/investigator
```

### Success Response

```json
{
  "assignedClaims": 22,
  "activeInvestigations": 15,
  "completedInvestigations": 180,
  "pendingTasks": 9
}
```

---

## Investigator Workload

### Endpoint

```http
GET /api/v1/dashboard/investigator/workload
```

### Success Response

```json
{
  "currentAssignments": 22,
  "capacity": 50,
  "utilizationPercentage": 44
}
```

---

## Investigator Task Summary

### Endpoint

```http
GET /api/v1/dashboard/investigator/tasks
```

### Success Response

```json
{
  "openTasks": 12,
  "completedTasks": 220
}
```

---

# 11. Real-Time Monitoring APIs

---

## System Health Dashboard

### Endpoint

```http
GET /api/v1/dashboard/system-health
```

### Success Response

```json
{
  "status": "HEALTHY",
  "database": "UP",
  "redis": "UP",
  "ocrService": "UP",
  "analysisService": "UP"
}
```

---

## Live Activity Feed

### Endpoint

```http
GET /api/v1/dashboard/live-activity
```

### Success Response

```json
[
  {
    "eventType": "CLAIM_CREATED",
    "timestamp": "2026-06-01T12:00:00Z"
  }
]
```

---

# 12. Dashboard Widget APIs

Widgets support dynamic frontend dashboards.

---

## Get Available Widgets

### Endpoint

```http
GET /api/v1/dashboard/widgets
```

### Success Response

```json
[
  {
    "widgetCode": "TOTAL_CLAIMS"
  },
  {
    "widgetCode": "FRAUD_ALERTS"
  }
]
```

---

## Get Widget Data

### Endpoint

```http
GET /api/v1/dashboard/widgets/{widgetCode}
```

### Success Response

```json
{
  "widgetCode": "TOTAL_CLAIMS",
  "value": 15000
}
```

---

# 13. Dashboard Export APIs

---

## Export Dashboard

### Endpoint

```http
GET /api/v1/dashboard/export
```

### Permissions

```text
KPI_EXPORT
```

### Query Parameters

```text
dashboardType
format
```

Formats:

```text
CSV
XLSX
PDF
```

### Success Response

```http
202 Accepted
```

### Audit Event

```text
DASHBOARD_EXPORTED
```

---

# 14. KPI Catalog

Supported KPIs

```text
TOTAL_CLAIMS
ACTIVE_CLAIMS
CLAIMS_APPROVED
CLAIMS_REJECTED

ACTIVE_INVESTIGATIONS
COMPLETED_INVESTIGATIONS

HIGH_RISK_CLAIMS
CRITICAL_RISK_CLAIMS

AVERAGE_FRAUD_SCORE

AVERAGE_RESOLUTION_TIME

OCR_SUCCESS_RATE
ANALYSIS_SUCCESS_RATE

ASSIGNMENT_QUEUE_SIZE

FAILED_NOTIFICATION_COUNT
```

---

# 15. Business Validation Rules

## Dashboard Access

* Dashboard visibility permission-based.
* Tenant isolation mandatory.

## Exports

* Export size limits configurable.
* Export operations audited.

## Widgets

* Widgets must support dashboard filters.
* Widget data cached where appropriate.

---

# 16. Error Codes

| Error Code               | Description           |
| ------------------------ | --------------------- |
| DASHBOARD_NOT_FOUND      | Dashboard missing     |
| KPI_NOT_FOUND            | KPI missing           |
| WIDGET_NOT_FOUND         | Widget missing        |
| EXPORT_NOT_SUPPORTED     | Invalid export format |
| FILTER_VALIDATION_FAILED | Invalid filter        |
| DASHBOARD_ACCESS_DENIED  | Access denied         |

---

# 17. Audit Events

```text
DASHBOARD_VIEWED
DASHBOARD_EXPORTED
KPI_VIEWED
WIDGET_VIEWED
```

---

# 18. Integration Events

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

Data Sources:

```text
analytics_snapshot
dashboard_metric
```

---

# 19. Performance Requirements

| API                      | Target   |
| ------------------------ | -------- |
| Dashboard Summary        | < 300 ms |
| KPI Cards                | < 200 ms |
| Widget Data              | < 200 ms |
| Trend Analysis           | < 500 ms |
| Dashboard Export Request | < 500 ms |
| Live Activity Feed       | < 300 ms |

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

* Executive dashboards restricted to leadership roles.
* Fraud dashboards restricted to fraud analysts.
* Investigator dashboards restricted to assigned users.
* Cross-tenant analytics access prohibited.

---

# 21. Caching Strategy

Recommended Cache TTL:

| Dashboard    | Cache      |
| ------------ | ---------- |
| Executive    | 5 Minutes  |
| Claims       | 2 Minutes  |
| Fraud        | 2 Minutes  |
| Operations   | 30 Seconds |
| Investigator | 30 Seconds |

Redis used for dashboard caching.

---

# Approval

This document defines the Dashboard & KPI API contract for ClaimLens and serves as the implementation reference for executive reporting, operational dashboards, fraud monitoring, investigator workspaces, KPI aggregation, dashboard widgets, exports, and real-time monitoring.
