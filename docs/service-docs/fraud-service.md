# 09.7 Fraud Service Design

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | RiskLens Technologies |
| Version         | V1                    |
| Document Type   | Service Design        |
| Document Number | 09.7                  |
| Document Name   | Fraud Service Design  |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

The Fraud Module is responsible for fraud risk evaluation and fraud alert generation.

Responsibilities:

* Fraud Evaluation
* Rule Execution
* Fraud Scoring
* Risk Classification
* Fraud Alert Creation
* Fraud Review Workflow
* Manual Overrides
* Fraud Analytics Support

The Fraud Module is the primary decision engine of ClaimLens.

---

# 2. Module Dependencies

## Consumed Events

```text
FRAUD_EVALUATION_QUEUED

INVESTIGATION_REPORT_SUBMITTED

INVESTIGATION_COMPLETED

DOCUMENT_VERSION_CREATED
```

---

## Published Events

```text
FRAUD_EVALUATION_STARTED

FRAUD_EVALUATED

FRAUD_SCORE_CHANGED

FRAUD_ALERT_CREATED

FRAUD_ALERT_ACKNOWLEDGED

FRAUD_ALERT_CLOSED

FRAUD_REVIEW_COMPLETED

FRAUD_SCORE_OVERRIDDEN
```

---

# 3. Package Structure

```text
fraud

├── controller
│   └── FraudController

├── service
│   ├── FraudService
│   ├── FraudEvaluationEngine
│   ├── FraudRuleEngine
│   ├── FraudScoreService
│   ├── FraudReviewService
│   └── FraudServiceImpl

├── repository
│   ├── FraudScoreRepository
│   ├── FraudAlertRepository
│   ├── FraudRuleExecutionRepository
│   └── FraudPolicyRepository

├── entity
│   ├── FraudScore
│   ├── FraudAlert
│   └── FraudRuleExecution

├── dto

├── mapper

├── validator

├── event

├── exception
```

---

# 4. Domain Entities

Primary Entities

```text
FraudScore

FraudAlert

FraudRuleExecution
```

---

# 5. Fraud Evaluation Engine

Responsibilities:

```text
Collect Inputs

Execute Rules

Calculate Score

Classify Risk

Generate Alerts
```

---

## Evaluation Flow

```text
FRAUD_EVALUATION_QUEUED
          ↓
Build Claim Snapshot
          ↓
Execute Rules
          ↓
Calculate Score
          ↓
Classify Risk
          ↓
Persist Results
          ↓
Publish Events
```

---

# 6. Claim Snapshot Builder

Before evaluation:

```text
Claim Snapshot
```

must be constructed.

Sources:

```text
Claim

Active Document Versions

OCR Results

Analysis Results

Processing State
```

---

## Critical Rule

Only Active Versions Used

Example:

```text
DL V1

RC V2

POLICY V1
```

Never:

```text
RC V1
```

if RC V2 is active.

---

# 7. Fraud Rule Engine

Responsibilities:

```text
Load Rules

Execute Rules

Track Results

Generate Rule Executions
```

---

## Rule Execution Output

```java
ruleCode

ruleName

result

scoreImpact

reason
```

---

# 8. Fraud Score Service

Purpose:

Calculate final fraud score.

---

## Example

```text
Image Tampering      +25

Duplicate Document  +20

Missing Metadata    +10

Total Score         =55
```

---

## Range

```text
0-100
```

---

# 9. Risk Classification

## V1 Thresholds

```text
0-29
LOW

30-59
MEDIUM

60-79
HIGH

80-100
CRITICAL
```

---

## Configuration

Thresholds stored in:

```text
fraud_policy
```

Not hardcoded.

---

# 10. Alert Generation Strategy

Create Alert When:

```text
HIGH
OR
CRITICAL
```

---

## Workflow

```text
FRAUD_EVALUATED
          ↓
Threshold Check
          ↓
FRAUD_ALERT_CREATED
```

---

# 11. Fraud Review Service

Responsibilities:

```text
Investigation Review

Final Fraud Decision

Alert Closure
```

---

## Input

```text
Investigation Report
```

---

## Decisions

```text
CONFIRMED_FRAUD

NOT_FRAUD

INSUFFICIENT_EVIDENCE
```

---

# 12. Controller Layer

## FraudController

Base Path

```http
/api/v1/fraud
```

---

## Endpoints

```http
GET  /fraud/claims/{claimId}

GET  /fraud/alerts

GET  /fraud/alerts/{alertId}

POST /fraud/alerts/{alertId}/acknowledge

POST /fraud/alerts/{alertId}/close

POST /fraud/claims/{claimId}/override
```

---

# 13. Service Layer

## FraudService

```java
public interface FraudService
```

---

## Methods

### Evaluate Claim

```java
FraudEvaluationResponse evaluate(
    Long claimId
);
```

---

### Acknowledge Alert

```java
void acknowledgeAlert(
    Long alertId
);
```

---

### Close Alert

```java
void closeAlert(
    Long alertId,
    CloseFraudAlertRequest request
);
```

---

### Override Score

```java
void overrideScore(
    Long claimId,
    OverrideFraudScoreRequest request
);
```

---

# 14. DTO Design

## FraudEvaluationResponse

```java
claimId

fraudScore

riskLevel

evaluatedRules
```

---

## OverrideFraudScoreRequest

```java
newScore

reason
```

---

## FraudAlertResponse

```java
alertId

claimId

fraudScore

riskLevel

status
```

---

# 15. Repository Layer

## FraudScoreRepository

```java
extends JpaRepository<
    FraudScore,
    Long
>
```

---

## Methods

```java
Optional<FraudScore>
findByClaimId(Long claimId);
```

---

## FraudAlertRepository

```java
List<FraudAlert>
findByStatus(
    FraudAlertStatus status
);
```

---

# 16. Validation Layer

## FraudValidator

Responsibilities:

```text
Rule Validation

Threshold Validation

Override Validation

Alert Validation
```

---

## Override Rules

Required:

```text
Authorized User

Reason Provided
```

---

# 17. Event Publishing

## FraudEventPublisher

Events:

```text
FRAUD_EVALUATION_STARTED

FRAUD_EVALUATED

FRAUD_SCORE_CHANGED

FRAUD_ALERT_CREATED

FRAUD_ALERT_ACKNOWLEDGED

FRAUD_ALERT_CLOSED

FRAUD_REVIEW_COMPLETED

FRAUD_SCORE_OVERRIDDEN
```

---

All events use:

```text
Outbox Pattern
```

---

# 18. Transaction Boundaries

## Evaluate Fraud

```java
@Transactional
```

Workflow:

```text
Build Snapshot
       ↓
Execute Rules
       ↓
Calculate Score
       ↓
Persist Score
       ↓
Persist Rule Results
       ↓
Create Alert
       ↓
Save Events
       ↓
Commit
```

---

## Override Score

```java
@Transactional
```

Workflow:

```text
Validate User
       ↓
Update Score
       ↓
Create Audit Trail
       ↓
Save Event
       ↓
Commit
```

---

# 19. Exception Handling

Exceptions:

```java
FraudScoreNotFoundException

FraudAlertNotFoundException

FraudEvaluationException

InvalidFraudOverrideException
```

---

# 20. Security Rules

Required Permissions:

```text
FRAUD_VIEW

FRAUD_REVIEW

FRAUD_OVERRIDE
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
Fraud Evaluation

Alert Creation

Alert Closure

Fraud Review

Score Override
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
claim_id

risk_level

alert_status

tenant_id
```

---

Fraud Evaluation Target:

```text
< 500ms
```

per claim.

---

# 23. Dependency Diagram

```text
Processing Orchestrator
          ↓
FraudEvaluationEngine
          ↓
FraudRuleEngine
          ↓
FraudScoreService
          ↓
Repositories
          ↓
PostgreSQL

FraudService
          ↓
FraudEventPublisher
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
Rule Execution

Score Calculation

Risk Classification

Alert Creation

Alert Closure

Fraud Review

Score Override
```

---

# 25. Future Enhancements

V2 Reserved:

```text
Machine Learning Models

Fraud Pattern Detection

Network Analysis

Behavioral Analytics

Fraud Graph Engine
```

---

# Approval

This document defines the Fraud Module implementation blueprint and serves as the reference for fraud evaluation, rule execution, scoring, risk classification, alert generation, review workflows, event publishing, transaction management, audit integration, and future scalability.
