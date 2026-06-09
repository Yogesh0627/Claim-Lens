# 08.8 Notification Events

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | Event Design          |
| Document Number | 08.8                  |
| Document Name   | Notification Events   |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

This document defines all events produced by the Notification Module.

The Notification Module is responsible for:

* In-App Notifications
* Email Notifications
* SMS Notifications
* Template Rendering
* Delivery Tracking
* Retry Processing
* Notification Analytics

The Notification Module consumes business events from all domains and transforms them into customer-facing or user-facing communications.

---

# 2. Notification Event Catalog

| Event                         | Category             |
| ----------------------------- | -------------------- |
| NOTIFICATION_CREATED          | Domain + Integration |
| TEMPLATE_RENDERED             | System               |
| NOTIFICATION_DELIVERY_STARTED | System               |
| NOTIFICATION_DELIVERED        | Integration          |
| NOTIFICATION_FAILED           | System               |
| NOTIFICATION_READ             | Domain               |
| NOTIFICATION_RETRY_QUEUED     | System               |

---

# 3. NOTIFICATION_CREATED

## Category

```text
Domain Event
Integration Event
```

---

## Trigger

A business event requires user communication.

---

## Producer

```text
Notification Module
```

---

## Consumers

```text
Delivery Worker
Analytics Module
Audit Module
```

---

## Source Events

```text
CLAIM_CREATED

CLAIM_STATUS_CHANGED

ASSIGNMENT_CREATED

INVESTIGATION_COMPLETED

FRAUD_ALERT_CREATED

PASSWORD_RESET_COMPLETED
```

---

## Payload

```json
{
  "notificationId": 1001,
  "userId": 101,
  "channel": "EMAIL",
  "templateCode": "CLAIM_CREATED"
}
```

---

## Business Rules

* Recipient must exist.
* Template must be active.
* Channel must be enabled.

---

# 4. TEMPLATE_RENDERED

## Category

```text
System Event
```

---

## Trigger

Notification template rendered successfully.

---

## Producer

```text
Notification Module
```

---

## Consumers

```text
Delivery Worker
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "notificationId": 1001,
  "templateCode": "CLAIM_CREATED"
}
```

---

## Example

Template:

```text
Claim {{claimNumber}} created.
```

Rendered:

```text
Claim CLM-2026-000001 created.
```

---

# 5. NOTIFICATION_DELIVERY_STARTED

## Category

```text
System Event
```

---

## Trigger

Delivery worker starts processing.

---

## Producer

```text
Notification Delivery Worker
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
  "notificationId": 1001,
  "channel": "EMAIL"
}
```

---

# 6. NOTIFICATION_DELIVERED

## Category

```text
Integration Event
```

---

## Trigger

Notification successfully delivered.

---

## Producer

```text
Notification Delivery Worker
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
  "notificationId": 1001,
  "deliveryId": 5001,
  "channel": "EMAIL",
  "deliveredAt": "2026-06-01T10:00:00Z"
}
```

---

## Delivery Channels

```text
EMAIL
SMS
IN_APP
```

---

# 7. NOTIFICATION_FAILED

## Category

```text
System Event
```

---

## Trigger

Notification delivery failed.

---

## Producer

```text
Notification Delivery Worker
```

---

## Consumers

```text
Retry Worker
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "notificationId": 1001,
  "channel": "EMAIL",
  "failureReason": "SMTP_TIMEOUT"
}
```

---

## Business Rules

Failed notifications become retry candidates.

---

# 8. NOTIFICATION_READ

## Category

```text
Domain Event
```

---

## Trigger

User reads notification.

---

## Producer

```text
Notification Module
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
  "notificationId": 1001,
  "readBy": 101,
  "readAt": "2026-06-01T11:00:00Z"
}
```

---

# 9. NOTIFICATION_RETRY_QUEUED

## Category

```text
System Event
```

---

## Trigger

Failed delivery queued for retry.

---

## Producer

```text
Retry Worker
```

---

## Consumers

```text
Notification Delivery Worker
Analytics Module
Audit Module
```

---

## Payload

```json
{
  "notificationId": 1001,
  "attemptNumber": 2
}
```

---

## Retry Schedule

```text
Attempt 1 Immediate

Attempt 2 1 Minute

Attempt 3 5 Minutes

Attempt 4 15 Minutes

Attempt 5 30 Minutes
```

---

# 10. Notification Workflow

## Standard Delivery Flow

```text
CLAIM_CREATED
        ↓
NOTIFICATION_CREATED
        ↓
TEMPLATE_RENDERED
        ↓
NOTIFICATION_DELIVERY_STARTED
        ↓
NOTIFICATION_DELIVERED
```

---

## Failure Flow

```text
NOTIFICATION_DELIVERY_STARTED
            ↓
NOTIFICATION_FAILED
            ↓
NOTIFICATION_RETRY_QUEUED
            ↓
NOTIFICATION_DELIVERY_STARTED
```

---

## Read Flow

```text
NOTIFICATION_DELIVERED
            ↓
User Opens Notification
            ↓
NOTIFICATION_READ
```

---

# 11. Notification Channel Rules

## EMAIL

```text
Requires Valid Email
```

---

## SMS

```text
Requires Valid Phone Number
```

---

## IN_APP

```text
Always Available
```

---

## Critical Notifications

Must always generate:

```text
IN_APP
```

even if:

```text
EMAIL Disabled

SMS Disabled
```

---

# 12. Template Rules

Templates must:

```text
Be Active

Match Channel

Support Variables
```

Example Variables:

```text
claimNumber

investigationNumber

fraudScore

customerNumber
```

---

# 13. Business Validation Rules

## Notification Creation

* Recipient exists.
* Template active.
* Channel enabled.

## Delivery

* Retry limit respected.
* Delivery tracked.

## Read Events

* Notification must exist.
* User must own notification.

---

# 14. Monitoring Metrics

```text
notifications_created_total

notifications_delivered_total

notifications_failed_total

notification_retry_total

notification_read_total

notification_delivery_duration
```

---

# 15. Security Requirements

Events must not contain:

```text
Email Content

SMS Content

Passwords

Reset Tokens

Authentication Secrets
```

Only identifiers and metadata.

---

# 16. Future Events (V2)

Reserved:

```text
PUSH_NOTIFICATION_SENT

WHATSAPP_NOTIFICATION_SENT

WEBHOOK_NOTIFICATION_SENT

NOTIFICATION_BOUNCED
```

---

# Approval

This document defines the Notification Event Catalog for ClaimLens and serves as the implementation reference for template rendering, communication workflows, delivery tracking, retry handling, analytics aggregation, and audit tracking.
