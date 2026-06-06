# 07.11 Fraud Detection & Risk Assessment API

## Document Information

| Field           | Value                                 |
| --------------- | ------------------------------------- |
| Project         | ClaimLens                             |
| Company         | RiskLens Technologies                 |
| Version         | V1                                    |
| Document Type   | API Design                            |
| Document Number | 07.11                                 |
| Document Name   | Fraud Detection & Risk Assessment API |
| Status          | Approved                              |
| Last Updated    | June 2026                             |

---

# 1. Overview

The Fraud Detection module is the intelligence layer of ClaimLens.

It evaluates insurance claims using:

* OCR Results
* OCR Field Extractions
* Analysis Results
* Claim Information
* Document Validation Results
* Investigation Findings
* Fraud Policies
* Fraud Rules

The Fraud Engine generates:

* Fraud Scores
* Fraud Alerts
* Risk Categories
* Rule Execution Results
* Fraud Recommendations

The Fraud Engine is triggered only by the Processing Orchestrator.

---

# 2. Domain Entities

Managed Entities:

```text
fraud_score
fraud_alert
fraud_rule_execution
```

Related Entities:

```text
claim
document
ocr_result
analysis_result
investigation
fraud_policy
fraud_rule
```

---

# 3. Fraud Evaluation Architecture

## Fraud Evaluation Flow

```text
Claim Processing Complete
          ↓
Processing Orchestrator
          ↓
Load Active Fraud Policy
          ↓
Execute Fraud Rules
          ↓
Calculate Fraud Score
          ↓
Generate Fraud Alerts
          ↓
Assign Risk Category
          ↓
Persist Results
```

---

## Fraud Engine Trigger Rule

Only:

```text
PROCESSING ORCHESTRATOR
```

may trigger fraud evaluation.

Never:

```text
OCR SERVICE
ANALYSIS SERVICE
INVESTIGATION SERVICE
```

---

# 4. Risk Categories

## Supported Risk Levels

```text
LOW
MEDIUM
HIGH
CRITICAL
```

---

## Default Thresholds

| Score    | Risk Level |
| -------- | ---------- |
| 0 - 39   | LOW        |
| 40 - 69  | MEDIUM     |
| 70 - 89  | HIGH       |
| 90 - 100 | CRITICAL   |

Actual thresholds are configurable through Fraud Policies.

---

# 5. Permission Matrix

| Permission           | Description              |
| -------------------- | ------------------------ |
| FRAUD_VIEW           | View fraud results       |
| FRAUD_REVIEW         | Review fraud findings    |
| FRAUD_OVERRIDE       | Override fraud decisions |
| FRAUD_POLICY_VIEW    | View fraud policy        |
| FRAUD_DASHBOARD_VIEW | View fraud dashboard     |

---

# 6. Fraud Score APIs

---

## Get Fraud Score

### Endpoint

```http
GET /api/v1/fraud/claims/{claimId}/score
```

### Permissions

```text
FRAUD_VIEW
```

### Success Response

```json
{
  "claimId": 10001,
  "fraudScore": 78,
  "riskLevel": "HIGH",
  "evaluatedAt": "2026-06-01T12:00:00Z"
}
```

---

## Get Fraud Score History

### Endpoint

```http
GET /api/v1/fraud/claims/{claimId}/score-history
```

### Success Response

```json
[
  {
    "fraudScore": 65,
    "riskLevel": "MEDIUM",
    "evaluatedAt": "2026-06-01T10:00:00Z"
  },
  {
    "fraudScore": 78,
    "riskLevel": "HIGH",
    "evaluatedAt": "2026-06-01T12:00:00Z"
  }
]
```

---

# 7. Fraud Alert APIs

---

## Get Fraud Alerts

### Endpoint

```http
GET /api/v1/fraud/claims/{claimId}/alerts
```

### Success Response

```json
[
  {
    "alertId": 1001,
    "alertType": "IMAGE_TAMPERING",
    "severity": "HIGH",
    "message": "Potential image manipulation detected."
  }
]
```

---

## Get Fraud Alert

### Endpoint

```http
GET /api/v1/fraud/alerts/{alertId}
```

### Success Response

```json
{
  "alertId": 1001,
  "claimId": 10001,
  "alertType": "IMAGE_TAMPERING",
  "severity": "HIGH",
  "status": "OPEN",
  "createdAt": "2026-06-01T12:00:00Z"
}
```

---

## Acknowledge Fraud Alert

### Endpoint

```http
PATCH /api/v1/fraud/alerts/{alertId}/acknowledge
```

### Permissions

```text
FRAUD_REVIEW
```

### Request Body

```json
{
  "comments": "Investigation initiated."
}
```

### Success Response

```json
{
  "status": "ACKNOWLEDGED"
}
```

### Audit Event

```text
FRAUD_ALERT_ACKNOWLEDGED
```

---

## Close Fraud Alert

### Endpoint

```http
PATCH /api/v1/fraud/alerts/{alertId}/close
```

### Request Body

```json
{
  "resolution": "False positive"
}
```

### Success Response

```json
{
  "status": "CLOSED"
}
```

### Audit Event

```text
FRAUD_ALERT_CLOSED
```

---

# 8. Fraud Rule Execution APIs

---

## Get Rule Executions

### Endpoint

```http
GET /api/v1/fraud/claims/{claimId}/rule-executions
```

### Success Response

```json
[
  {
    "ruleCode": "IMAGE_TAMPERING",
    "executed": true,
    "matched": true,
    "riskWeight": 20,
    "scoreContribution": 20
  }
]
```

---

## Get Rule Execution Details

### Endpoint

```http
GET /api/v1/fraud/rule-executions/{executionId}
```

### Success Response

```json
{
  "executionId": 5001,
  "ruleCode": "OCR_CONFIDENCE_LOW",
  "matched": true,
  "scoreContribution": 15,
  "executedAt": "2026-06-01T12:00:00Z"
}
```

---

# 9. Fraud Evaluation APIs

---

## Trigger Fraud Evaluation

### Endpoint

```http
POST /api/v1/fraud/claims/{claimId}/evaluate
```

### Permissions

```text
FRAUD_REVIEW
```

### Description

Manual fraud re-evaluation.

### Business Rules

* Only administrators may manually evaluate.
* Active fraud policy required.

### Success Response

```json
{
  "message": "Fraud evaluation queued"
}
```

### Audit Event

```text
FRAUD_EVALUATION_REQUESTED
```

---

## Get Evaluation Status

### Endpoint

```http
GET /api/v1/fraud/claims/{claimId}/evaluation-status
```

### Success Response

```json
{
  "status": "COMPLETE",
  "evaluatedAt": "2026-06-01T12:00:00Z"
}
```

---

# 10. Fraud Review APIs

---

## Submit Fraud Review

### Endpoint

```http
POST /api/v1/fraud/claims/{claimId}/reviews
```

### Request Body

```json
{
  "reviewDecision": "CONFIRMED_FRAUD",
  "reviewNotes": "Evidence supports fraud suspicion."
}
```

### Success Response

```json
{
  "reviewId": 2001
}
```

### Audit Event

```text
FRAUD_REVIEW_SUBMITTED
```

---

## Get Fraud Review

### Endpoint

```http
GET /api/v1/fraud/claims/{claimId}/reviews
```

### Success Response

```json
{
  "reviewDecision": "CONFIRMED_FRAUD",
  "reviewedBy": "EMP0001",
  "reviewedAt": "2026-06-02T10:00:00Z"
}
```

---

# 11. Manual Override APIs

---

## Override Fraud Score

### Endpoint

```http
POST /api/v1/fraud/claims/{claimId}/override
```

### Permissions

```text
FRAUD_OVERRIDE
```

### Request Body

```json
{
  "newFraudScore": 45,
  "reason": "Manual review completed"
}
```

### Success Response

```json
{
  "fraudScore": 45,
  "riskLevel": "MEDIUM"
}
```

### Audit Event

```text
FRAUD_SCORE_OVERRIDDEN
```

---

## Get Override History

### Endpoint

```http
GET /api/v1/fraud/claims/{claimId}/override-history
```

### Success Response

```json
[
  {
    "oldFraudScore": 78,
    "newFraudScore": 45,
    "reason": "Manual review completed"
  }
]
```

---

# 12. Fraud Dashboard APIs

---

## Fraud Dashboard Summary

### Endpoint

```http
GET /api/v1/fraud/dashboard/summary
```

### Permissions

```text
FRAUD_DASHBOARD_VIEW
```

### Success Response

```json
{
  "totalClaimsEvaluated": 10000,
  "highRiskClaims": 520,
  "criticalRiskClaims": 115,
  "averageFraudScore": 38.4
}
```

---

## Fraud Trends

### Endpoint

```http
GET /api/v1/fraud/dashboard/trends
```

### Query Parameters

```text
fromDate
toDate
```

### Success Response

```json
{
  "dailyEvaluations": [
    {
      "date": "2026-06-01",
      "claimsEvaluated": 120
    }
  ]
}
```

---

## Top Fraud Rules

### Endpoint

```http
GET /api/v1/fraud/dashboard/top-rules
```

### Success Response

```json
[
  {
    "ruleCode": "IMAGE_TAMPERING",
    "triggerCount": 420
  }
]
```

---

# 13. Fraud Investigation Integration APIs

---

## Get Fraud Investigation Summary

### Endpoint

```http
GET /api/v1/fraud/claims/{claimId}/investigation-summary
```

### Success Response

```json
{
  "fraudScore": 78,
  "riskLevel": "HIGH",
  "alertCount": 3,
  "openAlerts": 1,
  "investigationStatus": "IN_PROGRESS"
}
```

---

# 14. Business Validation Rules

## Fraud Evaluation

* Active fraud policy required.
* Evaluation only against active document versions.
* Evaluation must use latest OCR and Analysis results.

## Fraud Alerts

* Alerts automatically generated by fraud engine.
* Closed alerts cannot be modified.

## Manual Override

* Reason mandatory.
* Override fully audited.
* Original score retained for history.

## Reviews

* One active review per claim.
* Reviewer must have fraud review permissions.

---

# 15. Error Codes

| Error Code                     | Description         |
| ------------------------------ | ------------------- |
| FRAUD_SCORE_NOT_FOUND          | Fraud score missing |
| FRAUD_ALERT_NOT_FOUND          | Alert missing       |
| FRAUD_RULE_EXECUTION_NOT_FOUND | Execution missing   |
| FRAUD_POLICY_NOT_FOUND         | Policy missing      |
| FRAUD_EVALUATION_IN_PROGRESS   | Evaluation running  |
| INVALID_OVERRIDE_SCORE         | Invalid score       |
| OVERRIDE_NOT_ALLOWED           | Override blocked    |
| REVIEW_ALREADY_EXISTS          | Review exists       |
| FRAUD_EVALUATION_BLOCKED       | Validation failed   |

---

# 16. Audit Events

```text
FRAUD_EVALUATION_REQUESTED

FRAUD_ALERT_ACKNOWLEDGED
FRAUD_ALERT_CLOSED

FRAUD_REVIEW_SUBMITTED

FRAUD_SCORE_OVERRIDDEN
```

---

# 17. Integration Events

Produced Events:

```text
FRAUD_EVALUATED
FRAUD_SCORE_CHANGED
FRAUD_ALERT_CREATED
FRAUD_ALERT_CLOSED
FRAUD_REVIEW_COMPLETED
```

Consumed By:

```text
PROCESSING MODULE
CLAIM MODULE
INVESTIGATION MODULE
NOTIFICATION MODULE
ANALYTICS MODULE
```

---

# 18. Performance Requirements

| API                   | Target   |
| --------------------- | -------- |
| Fraud Score Lookup    | < 200 ms |
| Fraud Alert Lookup    | < 200 ms |
| Rule Execution Lookup | < 300 ms |
| Fraud Dashboard       | < 500 ms |
| Fraud Trends          | < 500 ms |
| Manual Override       | < 300 ms |

---

# 19. Security Requirements

All APIs require:

```text
JWT Authentication
Tenant Validation
Permission Validation
Audit Logging
```

Additional Controls:

* Fraud overrides restricted to authorized managers.
* Fraud reviews fully auditable.
* Cross-tenant fraud access prohibited.
* Rule execution details protected from unauthorized users.

---

# 20. Fraud Engine Scoring Notes

Fraud Score Calculation:

```text
Rule Match
      ↓
Risk Weight Applied
      ↓
Score Contribution Added
      ↓
Aggregate Score Calculated
      ↓
Risk Category Assigned
```

Score Range:

```text
0 - 100
```

Policy-driven thresholds determine risk categorization.

---

# Approval

This document defines the Fraud Detection & Risk Assessment API contract for ClaimLens and serves as the implementation reference for fraud scoring, fraud alerts, fraud rule execution tracking, fraud reviews, manual overrides, dashboard analytics, and fraud investigation workflows.
