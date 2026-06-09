# Database Design Part 11 - Fraud Domain

Status: Draft

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* fraud_score
* fraud_alert
* fraud_rule_execution

The Fraud Domain is responsible for:

* Risk Scoring
* Rule Evaluation
* Fraud Detection
* Alert Generation
* Claim Escalation

This domain consumes outputs from:

* OCR Processing
* Image Analysis
* Claim Data
* Policy Engine

and produces:

* Fraud Scores
* Fraud Alerts
* Risk Levels

used by Assignment and Investigation workflows.

---

# Fraud Processing Flow

Claim Submitted

↓

OCR Complete

↓

Image Analysis Complete

↓

Claim Snapshot Built

↓

Fraud Rules Executed

↓

Fraud Score Calculated

↓

Fraud Alerts Created

↓

Risk Level Determined

↓

Assignment Engine

---

# Fraud Risk Model

Risk Levels

```text id="r1"
LOW

MEDIUM

HIGH
```

Example

```text id="r2"
0 - 39      LOW

40 - 69     MEDIUM

70+         HIGH
```

Thresholds are configurable via:

```text id="r3"
fraud_policy
```

---

# Table: fraud_score

Purpose:

Stores the final fraud evaluation result for a claim.

Schema:

public

---

Columns

| Column          | Type        | Constraints   |
| --------------- | ----------- | ------------- |
| id              | BIGSERIAL   | PRIMARY KEY   |
| tenant_id       | BIGINT      | NOT NULL      |
| claim_id        | BIGINT      | NOT NULL      |
| fraud_policy_id | BIGINT      | NOT NULL      |
| total_score     | INTEGER     | NOT NULL      |
| risk_level      | VARCHAR(50) | NOT NULL      |
| auto_escalated  | BOOLEAN     | DEFAULT FALSE |
| evaluated_at    | TIMESTAMP   | NOT NULL      |
| created_at      | TIMESTAMP   | NOT NULL      |

---

Foreign Keys

```sql id="f1"
claim_id
    → claim(id)

fraud_policy_id
    → fraud_policy(id)
```

---

Risk Levels

```text id="f2"
LOW

MEDIUM

HIGH
```

---

Indexes

```sql id="f3"
CREATE INDEX idx_fraud_score_claim
ON fraud_score(claim_id);

CREATE INDEX idx_fraud_score_risk_level
ON fraud_score(risk_level);

CREATE INDEX idx_fraud_score_evaluated_at
ON fraud_score(evaluated_at);
```

---

Business Rule

One active fraud score per claim version snapshot.

Future versions may store historical fraud score recalculations.

---

# Table: fraud_alert

Purpose:

Represents an individual fraud signal detected during evaluation.

A single claim may generate multiple fraud alerts.

Schema:

public

---

Columns

| Column         | Type         | Constraints |
| -------------- | ------------ | ----------- |
| id             | BIGSERIAL    | PRIMARY KEY |
| tenant_id      | BIGINT       | NOT NULL    |
| claim_id       | BIGINT       | NOT NULL    |
| fraud_score_id | BIGINT       | NOT NULL    |
| fraud_rule_id  | BIGINT       | NOT NULL    |
| severity       | VARCHAR(50)  | NOT NULL    |
| status         | VARCHAR(50)  | NOT NULL    |
| title          | VARCHAR(255) | NOT NULL    |
| description    | TEXT         |             |
| detected_value | TEXT         |             |
| expected_value | TEXT         |             |
| resolved_at    | TIMESTAMP    |             |
| resolved_by    | BIGINT       |             |
| created_at     | TIMESTAMP    | NOT NULL    |

---

Foreign Keys

```sql id="f4"
claim_id
    → claim(id)

fraud_score_id
    → fraud_score(id)

fraud_rule_id
    → fraud_rule(id)

resolved_by
    → user(id)
```

---

Severity Values

```text id="f5"
LOW

MEDIUM

HIGH

CRITICAL
```

---

Status Values

```text id="f6"
OPEN

UNDER_REVIEW

CONFIRMED

DISMISSED

RESOLVED
```

---

Indexes

```sql id="f7"
CREATE INDEX idx_fraud_alert_claim
ON fraud_alert(claim_id);

CREATE INDEX idx_fraud_alert_status
ON fraud_alert(status);

CREATE INDEX idx_fraud_alert_severity
ON fraud_alert(severity);

CREATE INDEX idx_fraud_alert_rule
ON fraud_alert(fraud_rule_id);
```

---

Examples

```text id="f8"
Duplicate Image Detected

Location Mismatch

Repair Cost Anomaly

Duplicate Claim Found

Missing Mandatory Document
```

---

# Table: fraud_rule_execution

Purpose:

Stores the execution result of each fraud rule.

Provides transparency and explainability.

Schema:

public

---

Columns

| Column             | Type        | Constraints |
| ------------------ | ----------- | ----------- |
| id                 | BIGSERIAL   | PRIMARY KEY |
| tenant_id          | BIGINT      | NOT NULL    |
| claim_id           | BIGINT      | NOT NULL    |
| fraud_score_id     | BIGINT      | NOT NULL    |
| fraud_rule_id      | BIGINT      | NOT NULL    |
| execution_status   | VARCHAR(50) | NOT NULL    |
| matched            | BOOLEAN     | NOT NULL    |
| score_contribution | INTEGER     | NOT NULL    |
| execution_time_ms  | BIGINT      |             |
| evaluation_details | JSONB       |             |
| executed_at        | TIMESTAMP   | NOT NULL    |

---

Foreign Keys

```sql id="f9"
claim_id
    → claim(id)

fraud_score_id
    → fraud_score(id)

fraud_rule_id
    → fraud_rule(id)
```

---

Execution Status

```text id="f10"
SUCCESS

FAILED

SKIPPED
```

---

Example Evaluation Details

```json id="f11"
{
  "rule": "REPAIR_COST_ANOMALY",
  "estimatedCost": 250000,
  "marketAverage": 180000,
  "deviationPercentage": 38
}
```

---

Indexes

```sql id="f12"
CREATE INDEX idx_fraud_execution_claim
ON fraud_rule_execution(claim_id);

CREATE INDEX idx_fraud_execution_rule
ON fraud_rule_execution(fraud_rule_id);

CREATE INDEX idx_fraud_execution_score
ON fraud_rule_execution(fraud_score_id);
```

---

# Fraud Escalation Logic

Fraud Score

↓

Risk Level

↓

Threshold Evaluation

↓

Auto Escalation

Example

```text id="f13"
Score = 92

Threshold = 85

Result:

Auto Escalated = TRUE
```

---

# Fraud Investigation Support

Investigators can view:

```text id="f14"
Fraud Score

Fraud Alerts

Rule Executions

Evidence

Explanation Details
```

This provides explainable fraud analysis.

---

# Fraud Domain ERD

Claim

↓

FraudScore

↓

FraudAlert

---

FraudScore

↓

FraudRuleExecution

---

FraudRule

↓

FraudAlert

---

FraudRule

↓

FraudRuleExecution

---

# Query Optimization Strategy

Most Common Queries

```sql id="f15"
WHERE claim_id = ?

WHERE risk_level = ?

WHERE status = 'OPEN'

WHERE severity = 'HIGH'

WHERE fraud_rule_id = ?
```

Indexes added accordingly.

---

# Future Enhancements

V2

```text id="f16"
Machine Learning Scoring

Anomaly Detection Models

Network Fraud Analysis

Claim Link Analysis

Behavioral Risk Scoring
```

---

# Fraud Domain Summary

Tables

```text id="f17"
fraud_score

fraud_alert

fraud_rule_execution
```

Supports

```text id="f18"
Rule-Based Fraud Detection      YES

Fraud Scoring                  YES

Fraud Alerts                   YES

Explainable Decisions          YES

Risk Categorization            YES

Auto Escalation                YES

Investigator Visibility        YES
```

Core Entity

```text id="f19"
fraud_score
```

Fraud Engine Ready

```text id="f20"
YES
```


# Database Design Part 12 - Notification Domain

Status: Draft

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* notification
* notification_template
* notification_delivery

The Notification Domain is responsible for:

* User Notifications
* Customer Notifications
* System Alerts
* Assignment Alerts
* Claim Status Updates
* Delivery Tracking
* Retry Handling

This domain integrates with:

* Claims
* Assignments
* Investigations
* Fraud Alerts
* Processing Jobs

---

# Notification Design Principles

Notifications are:

```text id="n1"
Asynchronous
```

They must never block:

```text id="n2"
Claim Creation

Assignment Creation

Investigation Updates
```

Notification delivery occurs through:

```text id="n3"
notification_job
↓
Notification Worker
↓
Delivery Channel
```

---

# Notification Flow

Business Event

↓

Notification Created

↓

Notification Job

↓

Notification Worker

↓

Email

OR

In-App Notification

↓

Delivery Status Updated

---

# Supported Channels (V1)

```text id="n4"
EMAIL

IN_APP
```

Future:

```text id="n5"
SMS

WHATSAPP

PUSH_NOTIFICATION
```

---

# Table: notification

Purpose:

Represents a notification generated by the system.

Schema:

public

---

Columns

| Column                   | Type         | Constraints |
| ------------------------ | ------------ | ----------- |
| id                       | BIGSERIAL    | PRIMARY KEY |
| tenant_id                | BIGINT       | NOT NULL    |
| recipient_user_id        | BIGINT       |             |
| recipient_customer_id    | BIGINT       |             |
| notification_template_id | BIGINT       |             |
| channel                  | VARCHAR(50)  | NOT NULL    |
| notification_type        | VARCHAR(100) | NOT NULL    |
| title                    | VARCHAR(500) | NOT NULL    |
| message                  | TEXT         | NOT NULL    |
| payload_json             | JSONB        |             |
| status                   | VARCHAR(50)  | NOT NULL    |
| read_at                  | TIMESTAMP    |             |
| created_at               | TIMESTAMP    | NOT NULL    |

---

Foreign Keys

```sql id="n6"
tenant_id
    → insurance_company(id)

recipient_user_id
    → user(id)

recipient_customer_id
    → customer(id)

notification_template_id
    → notification_template(id)
```

---

Business Rule

One notification can target:

```text id="n7"
User

OR

Customer
```

but not both simultaneously.

---

Notification Types

```text id="n8"
CLAIM_CREATED

CLAIM_ASSIGNED

CLAIM_APPROVED

CLAIM_REJECTED

DOCUMENT_REQUESTED

FRAUD_ALERT

ASSIGNMENT_ACCEPTED

ASSIGNMENT_REJECTED

INVESTIGATION_COMPLETED
```

---

Status Values

```text id="n9"
PENDING

SENT

FAILED

READ
```

---

Channel Values

```text id="n10"
EMAIL

IN_APP
```

---

Indexes

```sql id="n11"
CREATE INDEX idx_notification_user
ON notification(recipient_user_id);

CREATE INDEX idx_notification_customer
ON notification(recipient_customer_id);

CREATE INDEX idx_notification_status
ON notification(status);

CREATE INDEX idx_notification_created_at
ON notification(created_at);

CREATE INDEX idx_notification_tenant
ON notification(tenant_id);
```

---

# Table: notification_template

Purpose:

Reusable notification templates.

Schema:

public

---

Columns

| Column           | Type         | Constraints  |
| ---------------- | ------------ | ------------ |
| id               | BIGSERIAL    | PRIMARY KEY  |
| tenant_id        | BIGINT       | NOT NULL     |
| code             | VARCHAR(100) | NOT NULL     |
| name             | VARCHAR(255) | NOT NULL     |
| channel          | VARCHAR(50)  | NOT NULL     |
| subject_template | TEXT         |              |
| body_template    | TEXT         | NOT NULL     |
| is_active        | BOOLEAN      | DEFAULT TRUE |
| created_at       | TIMESTAMP    | NOT NULL     |
| updated_at       | TIMESTAMP    |              |

---

Foreign Keys

```sql id="n12"
tenant_id
    → insurance_company(id)
```

---

Unique Constraints

```sql id="n13"
CREATE UNIQUE INDEX uq_notification_template
ON notification_template(tenant_id, code);
```

---

Examples

```text id="n14"
CLAIM_CREATED_EMAIL

CLAIM_ASSIGNED_EMAIL

CLAIM_APPROVED_EMAIL

DOCUMENT_REQUESTED_EMAIL

FRAUD_ALERT_EMAIL
```

---

Template Example

Subject

```text id="n15"
Claim {{claimNumber}} Assigned
```

Body

```text id="n16"
Claim {{claimNumber}} has been assigned to you.
```

---

Indexes

```sql id="n17"
CREATE INDEX idx_notification_template_code
ON notification_template(code);
```

---

# Table: notification_delivery

Purpose:

Tracks actual delivery attempts.

Provides retry and audit support.

Schema:

public

---

Columns

| Column              | Type         | Constraints |
| ------------------- | ------------ | ----------- |
| id                  | BIGSERIAL    | PRIMARY KEY |
| tenant_id           | BIGINT       | NOT NULL    |
| notification_id     | BIGINT       | NOT NULL    |
| channel             | VARCHAR(50)  | NOT NULL    |
| delivery_status     | VARCHAR(50)  | NOT NULL    |
| attempt_count       | INTEGER      | DEFAULT 0   |
| provider_message_id | VARCHAR(255) |             |
| error_message       | TEXT         |             |
| last_attempt_at     | TIMESTAMP    |             |
| delivered_at        | TIMESTAMP    |             |
| created_at          | TIMESTAMP    | NOT NULL    |

---

Foreign Keys

```sql id="n18"
notification_id
    → notification(id)

tenant_id
    → insurance_company(id)
```

---

Delivery Status Values

```text id="n19"
PENDING

PROCESSING

DELIVERED

FAILED
```

---

Indexes

```sql id="n20"
CREATE INDEX idx_notification_delivery_notification
ON notification_delivery(notification_id);

CREATE INDEX idx_notification_delivery_status
ON notification_delivery(delivery_status);

CREATE INDEX idx_notification_delivery_attempts
ON notification_delivery(attempt_count);
```

---

# Retry Strategy

Notification Worker

↓

Delivery Attempt

↓

Failure

↓

Increment Attempt Count

↓

Retry

---

Example

```text id="n21"
Attempt 1

↓

Failed

↓

Attempt 2

↓

Failed

↓

Attempt 3

↓

Delivered
```

---

Maximum Retry Count

```text id="n22"
5
```

(Configurable)

---

# Dead Letter Strategy

If:

```text id="n23"
attempt_count > max_retries
```

↓

Mark

```text id="n24"
FAILED
```

↓

Create Audit Event

↓

Manual Review

---

# In-App Notifications

Displayed in:

```text id="n25"
Notification Center
```

Features:

```text id="n26"
Unread Count

Mark As Read

Notification History
```

---

# Email Notifications

Generated From:

```text id="n27"
notification_template
```

↓

Rendered

↓

Queued

↓

Delivered

---

# Notification Domain ERD

NotificationTemplate

↓

Notification

↓

NotificationDelivery

---

User

↓

Notification

---

Customer

↓

Notification

---

# Query Optimization Strategy

Most Common Queries

```sql id="n28"
WHERE recipient_user_id = ?

WHERE recipient_customer_id = ?

WHERE status = 'PENDING'

WHERE delivery_status = 'FAILED'

WHERE created_at >= ?
```

Indexes support these patterns.

---

# Future Enhancements

V2

```text id="n29"
SMS

WhatsApp

Push Notifications

Preference Management

Digest Notifications
```

---

# Notification Domain Summary

Tables

```text id="n30"
notification

notification_template

notification_delivery
```

Supports

```text id="n31"
Email Notifications           YES

In-App Notifications          YES

Retry Handling                YES

Delivery Tracking             YES

Template Management           YES

Read Tracking                 YES

Dead Letter Handling          YES

Auditability                  YES
```

Core Entity

```text id="n32"
notification
```

Notification Engine Ready

```text id="n33"
YES
```
