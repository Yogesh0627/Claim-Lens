# 09.8 Notification Service Design

## Document Information

| Field           | Value                       |
| --------------- | --------------------------- |
| Project         | ClaimLens                   |
| Company         | Niyo Technologies       |
| Version         | V1                          |
| Document Type   | Service Design              |
| Document Number | 09.8                        |
| Document Name   | Notification Service Design |
| Status          | Approved                    |
| Last Updated    | June 2026                   |

---

# 1. Overview

The Notification Module is responsible for delivering system communications to users.

Responsibilities:

* Notification Creation
* Template Rendering
* Email Delivery
* In-App Notifications
* Delivery Tracking
* Retry Handling
* Notification Preferences
* Notification History

The Notification Module consumes business events from all ClaimLens modules and transforms them into user-facing communications.

---

# 2. Module Dependencies

## Consumed Events

```text
CLAIM_CREATED

CLAIM_STATUS_CHANGED

ASSIGNMENT_CREATED

ASSIGNMENT_ESCALATED

INVESTIGATION_COMPLETED

FRAUD_ALERT_CREATED

REPORT_EXPORT_COMPLETED

AUDIT_EXPORT_COMPLETED
```

---

## Published Events

```text
NOTIFICATION_CREATED

TEMPLATE_RENDERED

NOTIFICATION_DELIVERY_STARTED

NOTIFICATION_DELIVERED

NOTIFICATION_FAILED

NOTIFICATION_READ

NOTIFICATION_RETRY_QUEUED
```

---

# 3. Package Structure

```text
notification

├── controller
│   └── NotificationController

├── service
│   ├── NotificationService
│   ├── TemplateService
│   ├── EmailService
│   ├── InAppNotificationService
│   ├── RetryService
│   └── NotificationServiceImpl

├── repository
│   ├── NotificationRepository
│   ├── NotificationTemplateRepository
│   └── NotificationDeliveryRepository

├── entity
│   ├── Notification
│   ├── NotificationTemplate
│   └── NotificationDelivery

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
Notification

NotificationTemplate

NotificationDelivery
```

---

# 5. Notification Service

Responsibilities:

```text
Create Notifications

Track Notification State

Read Tracking

Notification History
```

---

## Methods

```java
createNotification()

markAsRead()

getNotification()

getNotificationsByUser()
```

---

# 6. Template Service

Responsibilities:

```text
Load Templates

Render Variables

Validate Templates

Channel Resolution
```

---

## Example Template

```html
Claim {{claimNumber}} has been created successfully.
```

---

## Rendered

```html
Claim CLM-2026-000001 has been created successfully.
```

---

# 7. Email Service

Purpose:

```text
Email Delivery
```

---

## Provider

V1:

```text
AWS SES
```

---

Future:

```text
SendGrid

Mailgun
```

---

## Methods

```java
sendEmail()

validateEmail()

trackDelivery()
```

---

# 8. In-App Notification Service

Purpose:

```text
Dashboard Notifications
```

---

## Features

```text
Unread Count

Notification History

Read Tracking

Filtering
```

---

## Methods

```java
createInAppNotification()

markRead()

getUnreadCount()
```

---

# 9. Retry Service

Responsibilities:

```text
Retry Failed Deliveries

Track Attempts

Escalate Permanent Failures
```

---

## Retry Policy

```text
Attempt 1 Immediate

Attempt 2 1 Minute

Attempt 3 5 Minutes

Attempt 4 15 Minutes

Attempt 5 30 Minutes
```

---

## After Final Failure

Status:

```text
PERMANENT_FAILURE
```

---

# 10. Controller Layer

## NotificationController

Base Path

```http
/api/v1/notifications
```

---

## Endpoints

```http
GET   /notifications

GET   /notifications/{notificationId}

POST  /notifications/{notificationId}/read

GET   /notifications/unread/count
```

---

# 11. DTO Design

## NotificationResponse

```java
notificationId

title

message

channel

status

createdAt
```

---

## MarkReadRequest

```java
notificationId
```

---

## NotificationDeliveryResponse

```java
deliveryId

channel

status

attemptCount
```

---

# 12. Delivery Workflow

## Email

```text
Business Event
       ↓
Notification Created
       ↓
Template Rendered
       ↓
Email Service
       ↓
Delivered
```

---

## In-App

```text
Business Event
       ↓
Notification Created
       ↓
Persist Notification
       ↓
Visible To User
```

---

# 13. Repository Layer

## NotificationRepository

```java
extends JpaRepository<
    Notification,
    Long
>
```

---

## Methods

```java
List<Notification>
findByUserId(Long userId);
```

---

```java
long countByUserIdAndReadFalse(
    Long userId
);
```

---

## NotificationDeliveryRepository

Methods:

```java
findByNotificationId()

findFailedDeliveries()
```

---

# 14. Validation Layer

## NotificationValidator

Responsibilities:

```text
Recipient Validation

Channel Validation

Template Validation
```

---

## Rules

```text
Recipient Required

Template Active

Channel Enabled
```

---

# 15. Event Publishing

## NotificationEventPublisher

Events:

```text
NOTIFICATION_CREATED

TEMPLATE_RENDERED

NOTIFICATION_DELIVERY_STARTED

NOTIFICATION_DELIVERED

NOTIFICATION_FAILED

NOTIFICATION_READ

NOTIFICATION_RETRY_QUEUED
```

---

All events use:

```text
Outbox Pattern
```

---

# 16. Worker Design

## Delivery Worker

Runs:

```text
Every 10 Seconds
```

---

Responsibilities:

```text
Process Pending Deliveries

Invoke Channels

Track Results
```

---

## Retry Worker

Runs:

```text
Every 1 Minute
```

---

Responsibilities:

```text
Retry Failed Deliveries
```

---

# 17. Transaction Boundaries

## Create Notification

```java
@Transactional
```

Workflow:

```text
Create Notification
       ↓
Persist Notification
       ↓
Create Delivery Record
       ↓
Save Event
       ↓
Commit
```

---

## Mark Read

```java
@Transactional
```

Workflow:

```text
Load Notification
       ↓
Mark Read
       ↓
Save Event
       ↓
Commit
```

---

# 18. Exception Handling

Exceptions:

```java
NotificationNotFoundException

TemplateNotFoundException

EmailDeliveryException

NotificationValidationException
```

---

# 19. Security Rules

Required Permissions:

```text
NOTIFICATION_VIEW
```

---

User Access Rule:

```text
User May View
Only Own Notifications
```

---

Tenant Isolation:

```text
All Queries Filtered
By tenant_id
```

---

# 20. Audit Integration

Tracked:

```text
Notification Created

Notification Delivered

Notification Failed

Notification Read
```

---

Every write operation generates:

```text
AUDIT_EVENT_CREATED
```

---

# 21. Performance Considerations

Indexes:

```text
user_id

status

channel

tenant_id

created_at
```

---

Unread Count Target:

```text
< 100ms
```

---

Delivery Throughput Target:

```text
1000+
Notifications Per Minute
```

---

# 22. Dependency Diagram

```text
Business Events
        ↓
NotificationService
        ↓
TemplateService
        ↓
EmailService
        ↓
AWS SES

NotificationService
        ↓
InAppNotificationService
        ↓
PostgreSQL

NotificationService
        ↓
NotificationEventPublisher
        ↓
Outbox Table
```

---

# 23. Unit Testing Requirements

Coverage Target:

```text
90%+
```

Required Tests:

```text
Notification Creation

Template Rendering

Email Delivery

In-App Creation

Retry Logic

Read Tracking

Failure Handling
```

---

# 24. Future Enhancements

V2 Reserved:

```text
SMS Delivery

WhatsApp Integration

Push Notifications

User Preferences

Notification Digests
```

---

# Approval

This document defines the Notification Module implementation blueprint and serves as the reference for template rendering, email delivery, in-app notifications, retry handling, event publishing, transaction management, audit integration, and future scalability.
