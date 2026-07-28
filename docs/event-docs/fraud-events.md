> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; the authoritative behaviour is the code. Where this diverges, the code wins ([`../architecture.md`](../architecture.md), [`../domain-model.md`](../domain-model.md)); deltas flagged inline as **As-built** notes.

# 08.7 Fraud Events

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | Event Design          |
| Document Number | 08.7                  |
| Document Name   | Fraud Events          |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

This document defines all events produced by the Fraud Module.

The Fraud Module is responsible for:

* Fraud Evaluation
* Fraud Scoring
* Fraud Alert Generation
* Risk Classification
* Manual Investigation Escalation
* Fraud Review Workflows

The Fraud Module acts as the primary decision engine for claim risk assessment.

> **As-built (2026-07-29):** Fraud runs as a **synchronous engine**, not an event producer/consumer. The fraud worker calls `FraudEngine.evaluate()` (weighted rules → 0–100 score → risk band), persisting a `fraud_score` row that is explainable via `fraud_rule_execution`. None of the events below are emitted. The whole **alert domain was dropped**: there is no `FraudCase`/fraud-alert entity, and no `FRAUD_ALERT_CREATED/ACKNOWLEDGED/CLOSED`, `FRAUD_REVIEW_COMPLETED`, `FRAUD_SCORE_CHANGED/OVERRIDDEN` behaviour. A HIGH score does **not** auto-open an investigation; the claim simply proceeds to `AWAITING_ASSIGNMENT`, and whether fraud is confirmed is recorded by the investigator's decision (`claim.fraudConfirmed`, migration V32).

---

# 2. Fraud Event Catalog

| Event                    | Category             |
| ------------------------ | -------------------- |
| FRAUD_EVALUATION_STARTED | Integration          |
| FRAUD_EVALUATED          | Domain + Integration |
| FRAUD_SCORE_CHANGED      | Domain               |
| FRAUD_ALERT_CREATED      | Domain + Integration |
| FRAUD_ALERT_ACKNOWLEDGED | Domain               |
| FRAUD_ALERT_CLOSED       | Domain + Integration |
| FRAUD_REVIEW_COMPLETED   | Integration          |
| FRAUD_SCORE_OVERRIDDEN   | Domain               |

---

# 3. FRAUD_EVALUATION_STARTED

## Category

```text
Integration Event
```

---

## Trigger

Fraud evaluation begins.

---

## Producer

```text
Fraud Module
```

---

## Consumers

```text
Analytics Module
Audit Module
```

---

## Source Event

```text
FRAUD_EVALUATION_QUEUED
```

---

## Payload

```json
{
  "claimId": 10001,
  "evaluationId": 5001
}
```

---

# 4. FRAUD_EVALUATED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Fraud scoring completed.

---

## Producer

```text
Fraud Module
```

---

## Consumers

```text
Claim Module
Assignment Module
Notification Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "claimId": 10001,
  "fraudScore": 78,
  "riskLevel": "HIGH",
  "evaluatedRules": 24
}
```

---

## Risk Classification

```text
0-29   LOW
30-59  MEDIUM
60-79  HIGH
80-100 CRITICAL
```

> **As-built (2026-07-29):** The engine emits **three** bands, not four — `LOW / MEDIUM / HIGH` (there is **no `CRITICAL`**). Default thresholds are `MEDIUM ≥ 25`, `HIGH ≥ 50` (configurable per ruleset), not the 30/60/80 cutoffs shown. There is no `evaluatedRules`-carrying event; the rule breakdown lives in `fraud_rule_execution`.

---

# 5. FRAUD_SCORE_CHANGED

## Category

```text
Domain Event
```

---

## Trigger

Fraud score recalculated.

---

## Producer

```text
Fraud Module
```

---

## Consumers

```text
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "claimId": 10001,
  "oldScore": 65,
  "newScore": 78
}
```

---

# 6. FRAUD_ALERT_CREATED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Fraud threshold exceeded.

---

## Producer

```text
Fraud Module
```

---

## Consumers

```text
Assignment Module
Notification Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "fraudAlertId": 9001,
  "claimId": 10001,
  "fraudScore": 78,
  "riskLevel": "HIGH",
  "triggeredRules": [
    "IMAGE_TAMPERING",
    "DUPLICATE_DOCUMENT"
  ]
}
```

---

## Business Rules

Created when:

```text
riskLevel = HIGH
OR
riskLevel = CRITICAL
```

---

## Workflow

```text
FRAUD_ALERT_CREATED
          ↓
ASSIGNMENT_CREATED
          ↓
INVESTIGATION_CREATED
```

---

# 7. FRAUD_ALERT_ACKNOWLEDGED

## Category

```text
Domain Event
```

---

## Trigger

Investigator acknowledges fraud alert.

---

## Producer

```text
Fraud Module
```

---

## Consumers

```text
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "fraudAlertId": 9001,
  "acknowledgedBy": 101
}
```

---

# 8. FRAUD_ALERT_CLOSED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

Fraud alert resolved.

---

## Producer

```text
Fraud Module
```

---

## Consumers

```text
Claim Module
Notification Module
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "fraudAlertId": 9001,
  "claimId": 10001,
  "resolution": "FALSE_POSITIVE"
}
```

---

## Resolutions

```text
CONFIRMED_FRAUD

FALSE_POSITIVE

INSUFFICIENT_EVIDENCE
```

---

# 9. FRAUD_REVIEW_COMPLETED

## Category

```text
Integration Event
```

---

## Trigger

Investigation review completed.

---

## Producer

```text
Fraud Module
```

---

## Consumers

```text
Claim Module
Analytics Module
Audit Module
Notification Module
```

---

## Source Event

```text
INVESTIGATION_REPORT_SUBMITTED
```

---

## Payload

```json
{
  "claimId": 10001,
  "fraudAlertId": 9001,
  "finalDecision": "CONFIRMED_FRAUD"
}
```

---

## Decisions

```text
CONFIRMED_FRAUD

NOT_FRAUD

REQUIRES_MORE_REVIEW
```

---

# 10. FRAUD_SCORE_OVERRIDDEN

## Category

```text
Domain Event
```

---

## Trigger

Manual override performed.

---

## Producer

```text
Fraud Module
```

---

## Consumers

```text
Analytics Module
Audit Module
Compliance Module
```

---

## Payload

```json
{
  "claimId": 10001,
  "oldScore": 78,
  "newScore": 45,
  "reason": "Manual Review"
}
```

---

## Business Rules

* Justification required.
* Override fully audited.

---

# 11. Fraud Workflow

## Standard Fraud Evaluation

```text
CLAIM_PROCESSING_COMPLETED
          ↓
FRAUD_EVALUATION_QUEUED
          ↓
FRAUD_EVALUATION_STARTED
          ↓
FRAUD_EVALUATED
```

---

## High-Risk Flow

```text
FRAUD_EVALUATED
          ↓
FRAUD_ALERT_CREATED
          ↓
ASSIGNMENT_CREATED
          ↓
INVESTIGATION_CREATED
```

---

## Fraud Resolution Flow

```text
INVESTIGATION_COMPLETED
          ↓
FRAUD_REVIEW_COMPLETED
          ↓
FRAUD_ALERT_CLOSED
```

---

# 12. Business Validation Rules

## Evaluation

* Processing must be complete.
* Active document versions only.

## Alert Creation

* Risk threshold exceeded.
* Fraud score available.

## Override

* Authorized users only.
* Reason mandatory.

---

# 13. Monitoring Metrics

```text
fraud_evaluations_total

fraud_alerts_created_total

fraud_alerts_closed_total

fraud_overrides_total

average_fraud_score
```

---

# 14. Security Requirements

Events must not contain:

```text
Raw OCR Data
Analysis Payloads
Customer PII
Internal Fraud Algorithms
```

Only identifiers, scores, and metadata.

---

# 15. Future Events (V2)

Reserved:

```text
FRAUD_MODEL_UPDATED

FRAUD_MODEL_DEPLOYED

FRAUD_PATTERN_DETECTED

FRAUD_NETWORK_IDENTIFIED
```

---

# Approval

This document defines the Fraud Event Catalog for ClaimLens and serves as the implementation reference for fraud evaluation, risk classification, alert management, manual investigations, fraud review workflows, analytics aggregation, notifications, and audit tracking.
