# 07.12 Notification Management API

## Document Information

| Field           | Value                       |
| --------------- | --------------------------- |
| Project         | ClaimLens                   |
| Company         | RiskLens Technologies       |
| Version         | V1                          |
| Document Type   | API Design                  |
| Document Number | 07.12                       |
| Document Name   | Notification Management API |
| Status          | Approved                    |
| Last Updated    | June 2026                   |

---

# 1. Overview

The Notification Management module provides communication capabilities across the ClaimLens platform.

The module is responsible for:

* In-App Notifications
* Email Notifications
* SMS Notifications
* Notification Templates
* Notification Delivery Tracking
* Notification Preferences
* Event-Based Notification Processing
* Notification Analytics

Notifications are generated automatically based on business events occurring throughout the platform.

---

# 2. Domain Entities

Managed Entities:

```text id="yxk1n2"
notification
notification_template
notification_delivery
```

Related Entities:

```text id="xqf7w8"
user
claim
assignment
investigation
fraud_alert
```

---

# 3. Notification Architecture

## Event Driven Flow

```text id="vpxo6d"
Business Event
       ↓
Notification Event
       ↓
Template Resolution
       ↓
Notification Created
       ↓
Delivery Queue
       ↓
Email / SMS / In-App
       ↓
Delivery Tracking
```

---

## Supported Channels

```text id="avm8rk"
IN_APP
EMAIL
SMS
```

---

## Notification Priorities

```text id="jw8m2x"
LOW
MEDIUM
HIGH
CRITICAL
```

---

# 4. Permission Matrix

| Permission                   | Description          |
| ---------------------------- | -------------------- |
| NOTIFICATION_VIEW            | View notifications   |
| NOTIFICATION_MANAGE          | Manage notifications |
| NOTIFICATION_TEMPLATE_VIEW   | View templates       |
| NOTIFICATION_TEMPLATE_MANAGE | Manage templates     |
| NOTIFICATION_DELIVERY_VIEW   | View delivery logs   |

---

# 5. Notification APIs

---

## Get User Notifications

### Endpoint

```http id="tpc8zq"
GET /api/v1/notifications
```

### Permissions

```text id="v4g4yt"
NOTIFICATION_VIEW
```

### Query Parameters

```text id="zg75tu"
page
size
status
priority
channel
```

### Success Response

```json id="mqf3pl"
{
  "content": [
    {
      "notificationId": 1001,
      "title": "Claim Assigned",
      "status": "UNREAD",
      "priority": "HIGH",
      "createdAt": "2026-06-01T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20
}
```

---

## Get Notification

### Endpoint

```http id="v5v5u4"
GET /api/v1/notifications/{notificationId}
```

### Success Response

```json id="3f29f7"
{
  "notificationId": 1001,
  "title": "Claim Assigned",
  "message": "Claim CLM-2026-000001 assigned to you.",
  "status": "UNREAD",
  "channel": "IN_APP"
}
```

---

## Mark Notification As Read

### Endpoint

```http id="t6ldrm"
PATCH /api/v1/notifications/{notificationId}/read
```

### Success Response

```json id="1z7wde"
{
  "status": "READ"
}
```

### Audit Event

```text id="6mvv1h"
NOTIFICATION_READ
```

---

## Mark All Notifications As Read

### Endpoint

```http id="bkr1nv"
PATCH /api/v1/notifications/read-all
```

### Success Response

```json id="gkjq0k"
{
  "updatedCount": 35
}
```

---

## Delete Notification

### Endpoint

```http id="72jg4x"
DELETE /api/v1/notifications/{notificationId}
```

### Success Response

```http id="s9jvgs"
204 No Content
```

### Audit Event

```text id="8khh2h"
NOTIFICATION_DELETED
```

---

# 6. Notification Template APIs

---

## Create Notification Template

### Endpoint

```http id="3uk8fj"
POST /api/v1/notification-templates
```

### Permissions

```text id="6k7m4m"
NOTIFICATION_TEMPLATE_MANAGE
```

### Request Body

```json id="6p6hzx"
{
  "templateCode": "CLAIM_ASSIGNED",
  "templateName": "Claim Assignment Notification",
  "channel": "EMAIL",
  "subject": "Claim Assigned",
  "body": "Claim {{claimNumber}} has been assigned."
}
```

### Success Response

```json id="jlwmgs"
{
  "templateId": 101
}
```

### Audit Event

```text id="pxy93v"
NOTIFICATION_TEMPLATE_CREATED
```

---

## Get Template

### Endpoint

```http id="wrpd5i"
GET /api/v1/notification-templates/{templateId}
```

---

## Update Template

### Endpoint

```http id="wn6b4r"
PUT /api/v1/notification-templates/{templateId}
```

### Success Response

```json id="jbnj53"
{
  "message": "Template updated successfully"
}
```

### Audit Event

```text id="z9yr0q"
NOTIFICATION_TEMPLATE_UPDATED
```

---

## List Templates

### Endpoint

```http id="hbgj6q"
GET /api/v1/notification-templates
```

### Query Parameters

```text id="w4o5k0"
channel
status
templateCode
```

---

# 7. Notification Delivery APIs

---

## Get Notification Deliveries

### Endpoint

```http id="2n8y4e"
GET /api/v1/notification-deliveries
```

### Permissions

```text id="6knysb"
NOTIFICATION_DELIVERY_VIEW
```

### Query Parameters

```text id="w9o4j5"
notificationId
channel
status
```

### Success Response

```json id="g0zjtl"
[
  {
    "deliveryId": 5001,
    "channel": "EMAIL",
    "status": "DELIVERED",
    "deliveredAt": "2026-06-01T10:01:00Z"
  }
]
```

---

## Get Delivery Details

### Endpoint

```http id="wdw0p2"
GET /api/v1/notification-deliveries/{deliveryId}
```

### Success Response

```json id="e2pdzb"
{
  "deliveryId": 5001,
  "channel": "EMAIL",
  "status": "DELIVERED",
  "attemptCount": 1
}
```

---

## Retry Notification Delivery

### Endpoint

```http id="l7ktb6"
POST /api/v1/notification-deliveries/{deliveryId}/retry
```

### Success Response

```json id="54vq0s"
{
  "message": "Retry queued"
}
```

### Audit Event

```text id="3k5xv6"
NOTIFICATION_DELIVERY_RETRIED
```

---

# 8. Notification Preferences APIs

---

## Get User Preferences

### Endpoint

```http id="b8ln0f"
GET /api/v1/notifications/preferences
```

### Success Response

```json id="q5sy4q"
{
  "emailEnabled": true,
  "smsEnabled": false,
  "inAppEnabled": true
}
```

---

## Update User Preferences

### Endpoint

```http id="gm5gqf"
PUT /api/v1/notifications/preferences
```

### Request Body

```json id="kv6cth"
{
  "emailEnabled": true,
  "smsEnabled": true,
  "inAppEnabled": true
}
```

### Success Response

```json id="tgsgbi"
{
  "message": "Preferences updated successfully"
}
```

### Audit Event

```text id="tcz1wk"
NOTIFICATION_PREFERENCES_UPDATED
```

---

# 9. Manual Notification APIs

---

## Send Notification

### Endpoint

```http id="x7uqw6"
POST /api/v1/notifications/send
```

### Permissions

```text id="o44q3q"
NOTIFICATION_MANAGE
```

### Request Body

```json id="jfh8wo"
{
  "userIds": [1001,1002],
  "channel": "IN_APP",
  "title": "System Maintenance",
  "message": "Scheduled maintenance tonight."
}
```

### Success Response

```json id="0fwf9n"
{
  "notificationsCreated": 2
}
```

### Audit Event

```text id="mr7k2f"
MANUAL_NOTIFICATION_SENT
```

---

# 10. Notification Dashboard APIs

---

## Notification Summary

### Endpoint

```http id="m8txkx"
GET /api/v1/notifications/dashboard/summary
```

### Success Response

```json id="sgr1g4"
{
  "totalNotifications": 10000,
  "unreadNotifications": 240,
  "failedDeliveries": 18
}
```

---

## Delivery Statistics

### Endpoint

```http id="cg4xnm"
GET /api/v1/notifications/dashboard/delivery-statistics
```

### Success Response

```json id="2h04ji"
{
  "emailDeliveries": 5000,
  "smsDeliveries": 1200,
  "inAppDeliveries": 3800
}
```

---

## Delivery Failure Report

### Endpoint

```http id="kn5t8s"
GET /api/v1/notifications/dashboard/failures
```

### Success Response

```json id="m8x0l2"
[
  {
    "channel": "EMAIL",
    "failureCount": 18
  }
]
```

---

# 11. Event Trigger Catalog

## Claim Events

```text id="1wrlv8"
CLAIM_CREATED
CLAIM_STATUS_CHANGED
INFORMATION_REQUEST_CREATED
```

---

## Assignment Events

```text id="5dw9zk"
CLAIM_ASSIGNED
CLAIM_REASSIGNED
ASSIGNMENT_ACCEPTED
```

---

## Investigation Events

```text id="cm9c2w"
INVESTIGATION_CREATED
INVESTIGATION_COMPLETED
```

---

## Fraud Events

```text id="t8mgzq"
FRAUD_ALERT_CREATED
FRAUD_REVIEW_COMPLETED
```

---

## User Events

```text id="lc1v4y"
USER_CREATED
PASSWORD_RESET_COMPLETED
```

---

# 12. Delivery Status Lifecycle

Supported Statuses:

```text id="b4yq7d"
PENDING
PROCESSING
DELIVERED
FAILED
```

Lifecycle:

```text id="lwtlcf"
PENDING
   ↓
PROCESSING
   ↓
DELIVERED

OR

PROCESSING
   ↓
FAILED
```

---

# 13. Business Validation Rules

## Notification Creation

* Recipient must exist.
* Template must be active.
* Channel must be enabled.

## Template Management

* Template code unique within tenant.
* Active templates only used for delivery.

## Delivery

* Failed deliveries eligible for retry.
* Retry count limited by configuration.

## Preferences

* Users may disable email and SMS.
* Critical notifications always delivered in-app.

---

# 14. Error Codes

| Error Code             | Description          |
| ---------------------- | -------------------- |
| NOTIFICATION_NOT_FOUND | Notification missing |
| TEMPLATE_NOT_FOUND     | Template missing     |
| DELIVERY_NOT_FOUND     | Delivery missing     |
| INVALID_CHANNEL        | Channel invalid      |
| DELIVERY_FAILED        | Delivery failed      |
| TEMPLATE_INACTIVE      | Template inactive    |
| RECIPIENT_NOT_FOUND    | User missing         |
| RETRY_LIMIT_EXCEEDED   | Retry blocked        |

---

# 15. Audit Events

```text id="xjsmy0"
NOTIFICATION_READ
NOTIFICATION_DELETED

NOTIFICATION_TEMPLATE_CREATED
NOTIFICATION_TEMPLATE_UPDATED

NOTIFICATION_DELIVERY_RETRIED

NOTIFICATION_PREFERENCES_UPDATED

MANUAL_NOTIFICATION_SENT
```

---

# 16. Integration Events

Produced Events:

```text id="dzwnv0"
NOTIFICATION_CREATED
NOTIFICATION_DELIVERED
NOTIFICATION_FAILED
```

Consumed By:

```text id="0tqj3h"
CLAIM MODULE
ASSIGNMENT MODULE
INVESTIGATION MODULE
FRAUD MODULE
AUTH MODULE
```

---

# 17. Performance Requirements

| API                 | Target   |
| ------------------- | -------- |
| Notification Lookup | < 200 ms |
| Notification List   | < 300 ms |
| Template Lookup     | < 200 ms |
| Delivery Lookup     | < 300 ms |
| Dashboard APIs      | < 500 ms |
| Manual Notification | < 300 ms |

---

# 18. Security Requirements

All APIs require:

```text id="x9hrk0"
JWT Authentication
Tenant Validation
Permission Validation
Audit Logging
```

Additional Controls:

* Users may only access their own notifications.
* Template management restricted to administrators.
* Delivery logs restricted to authorized users.
* Cross-tenant notification access prohibited.

---

# 19. Notification Delivery Notes

Notification delivery is asynchronous.

Delivery workers process:

```text id="8o4k8x"
EMAIL
SMS
IN_APP
```

using queue-based processing.

Failures generate:

```text id="52x5gq"
notification_delivery
```

records for retry and audit tracking.

---

# Approval

This document defines the Notification Management API contract for ClaimLens and serves as the implementation reference for notifications, templates, delivery tracking, user preferences, event-driven communication, and notification analytics.
