# 09.1 Service Design Standards

## Document Information

| Field           | Value                    |
| --------------- | ------------------------ |
| Project         | ClaimLens                |
| Company         | Niyo Technologies    |
| Version         | V1                       |
| Document Type   | Service Design           |
| Document Number | 09.1                     |
| Document Name   | Service Design Standards |
| Status          | Approved                 |
| Last Updated    | June 2026                |

---

# 1. Overview

This document defines the implementation standards for all ClaimLens services.

The purpose of this document is to ensure:

* Consistent code structure
* Modular boundaries
* Maintainability
* Testability
* Scalability
* Future microservice migration readiness

All modules must follow these standards.

---

# 2. Architectural Style

ClaimLens V1 follows:

```text
Modular Monolith
```

Implementation Stack:

```text
Spring Boot 3.x
Java 21
PostgreSQL
Redis
Docker
AWS S3
```

Supporting Services:

```text
OCR Service (Python)

Analysis Service (Python)
```

---

# 3. Package Structure

Root Package

```text
com.Niyo.claimlens
```

---

## Module Structure

```text
com.Niyo.claimlens

├── claim
├── document
├── assignment
├── investigation
├── processing
├── fraud
├── notification
├── analytics
├── audit

├── auth
├── organization
├── user
├── policy

├── shared
├── common
├── security
├── config
```

---

# 4. Internal Module Structure

Every module must follow:

```text
claim

├── controller
├── service
├── repository
├── entity
├── dto
├── mapper
├── validator
├── event
├── exception
├── specification
```

Example:

```text
claim

├── controller
│   └── ClaimController

├── service
│   ├── ClaimService
│   └── ClaimServiceImpl

├── repository
│   └── ClaimRepository

├── entity
│   └── Claim

├── dto
│   ├── request
│   └── response

├── mapper
│   └── ClaimMapper

├── validator
│   └── ClaimValidator

├── event
│   └── ClaimEventPublisher
```

---

# 5. Layer Responsibilities

## Controller Layer

Responsible For:

```text
Request Handling

Authentication Context

Request Validation

Response Mapping
```

Controllers must NOT:

```text
Contain Business Logic

Call Repositories Directly

Publish Events
```

---

## Service Layer

Responsible For:

```text
Business Logic

Transaction Management

Workflow Coordination

Event Publishing
```

Services must NOT:

```text
Contain HTTP Logic

Build API Responses
```

---

## Repository Layer

Responsible For:

```text
Database Access

Queries

Specifications
```

Repositories must NOT:

```text
Contain Business Rules
```

---

## Mapper Layer

Responsible For:

```text
DTO ↔ Entity Conversion
```

Recommended:

```text
MapStruct
```

---

## Validator Layer

Responsible For:

```text
Business Validation

Cross-Entity Validation
```

Example:

```text
Claim Status Transition Validation
```

---

# 6. DTO Standards

---

## Request DTO

Used for:

```text
POST

PUT

PATCH
```

Example:

```java
CreateClaimRequest
```

---

## Response DTO

Used for:

```text
API Responses
```

Example:

```java
ClaimResponse
```

---

## Internal DTO

Used between modules.

Example:

```java
FraudEvaluationRequest
```

Never exposed externally.

---

# 7. Entity Standards

All entities inherit:

```java
BaseEntity
```

---

## BaseEntity

Contains:

```text
id

createdAt
createdBy

updatedAt
updatedBy

isDeleted

deletedAt
deletedBy
```

---

## Entity Rules

Entities must:

```text
Represent Database Tables

Avoid Business Logic

Avoid Service Dependencies
```

---

# 8. Repository Standards

Primary Pattern:

```java
JpaRepository
```

Example:

```java
ClaimRepository
    extends JpaRepository<Claim, Long>
```

---

## Complex Queries

Use:

```java
Specification
```

Pattern:

```java
ClaimSpecification
```

---

## Native Queries

Allowed only when:

```text
Performance Critical

Complex Reporting
```

---

# 9. Service Standards

Every service exposes:

```java
Interface

Implementation
```

Example:

```java
ClaimService

ClaimServiceImpl
```

---

## Benefits

```text
Testing

Mocking

Future Refactoring
```

---

# 10. Transaction Standards

---

## Allowed

```java
@Service

@Transactional
```

---

## Transaction Boundary

Must exist in:

```text
Service Layer
```

Only.

---

## Forbidden

```java
Controller

Repository
```

---

# 11. Event Publishing Standards

ClaimLens uses:

```text
Outbox Pattern
```

---

## Rule

Services never publish directly.

Wrong:

```java
eventPublisher.publish(...)
```

---

Correct:

```java
outboxEventService.save(...)
```

---

## Flow

```text
Business Transaction
        ↓
Database Commit
        ↓
Outbox Record
        ↓
Publisher Worker
        ↓
Consumer
```

---

# 12. Module Communication Rules

---

## Preferred

```text
Events
```

---

## Allowed

```text
Service → Service
```

when synchronous behavior required.

---

## Forbidden

```text
Controller → Controller
```

---

# 13. Validation Strategy

---

## Layer 1

Bean Validation

Example:

```java
@NotNull

@Size

@Email
```

---

## Layer 2

Business Validation

Example:

```text
Claim Cannot Move
From CLOSED
To UNDER_REVIEW
```

---

# 14. Exception Handling

Global Handler:

```java
GlobalExceptionHandler
```

---

## Exception Types

```java
ValidationException

BusinessException

ResourceNotFoundException

UnauthorizedException
```

---

## Standard Error Response

```json
{
  "timestamp": "2026-06-01T10:00:00Z",
  "errorCode": "CLAIM_NOT_FOUND",
  "message": "Claim not found"
}
```

---

# 15. Logging Standards

Framework:

```text
SLF4J
```

---

## Log Levels

```text
INFO

WARN

ERROR
```

---

## Never Log

```text
Passwords

Tokens

Secrets

Sensitive PII
```

---

# 16. Security Standards

Authentication:

```text
JWT
```

Authorization:

```text
Role Based Access Control
```

---

## Tenant Validation

Every request must validate:

```text
tenant_id
```

before data access.

---

# 17. Auditing Standards

Every write operation must generate:

```text
Audit Event

Audit Log
```

through the Audit Module.

---

# 18. API Response Standards

Success Response:

```json
{
  "success": true,
  "data": {}
}
```

---

Failure Response:

```json
{
  "success": false,
  "errorCode": "CLAIM_NOT_FOUND",
  "message": "Claim not found"
}
```

---

# 19. Testing Standards

Required:

```text
Unit Tests

Service Tests

Repository Tests

Controller Tests
```

---

Recommended Coverage:

```text
80%+
```

Service Layer Coverage:

```text
90%+
```

---

# 20. Performance Standards

Target:

```text
API Response < 300ms

Database Query < 100ms

Dashboard APIs < 500ms
```

---

# 21. Future Migration Rules

Modules must be designed so that:

```text
Claim Module

Fraud Module

Notification Module

Analytics Module
```

can become independent microservices later.

Therefore:

```text
No Cross-Module Entity References

No Shared Repositories

No Circular Dependencies
```

---

# 22. Service Design Deliverables

Each Service Design document must define:

```text
Module Overview

Package Structure

Entities

Repositories

DTOs

Services

Validators

Mappers

Event Publishers

Transaction Boundaries

Dependency Diagram
```

---

# Approval

This document defines the implementation standards for all ClaimLens modules and serves as the foundation for service design, module implementation, testing, event publishing, transaction management, security, auditing, and future scalability.
