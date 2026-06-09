# 10.1 Implementation Blueprint

**Project:** ClaimLens
**Company:** Niyo Technologies

---

# 1. Purpose

This document defines the implementation strategy for ClaimLens.

All architecture, database, API, event, and service design decisions are considered finalized.

This document serves as the execution blueprint for engineering teams and provides guidance for repository setup, module implementation, development standards, deployment preparation, and MVP delivery.

---

# 2. Repository Structure

```text
insurance-fraud-platform/

├── frontend/
│   ├── public/
│   ├── src/
│   ├── tests/
│   ├── package.json
│   └── Dockerfile
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── ocr-service/
│   ├── app/
│   ├── requirements.txt
│   └── Dockerfile
│
├── analysis-service/
│   ├── app/
│   ├── requirements.txt
│   └── Dockerfile
│
├── infra/
│   ├── docker/
│   ├── monitoring/
│   ├── scripts/
│   └── environments/
│
├── docs/
│
└── docker-compose.yml
```

---

# 3. Backend Module Layout

Base Package

```text
com.Niyo.claimlens
```

## Core Packages

```text
common
config
security
tenancy
events
outbox
scheduler
integration
```

## Business Modules

```text
organization
user
policy
claim
document
assignment
investigation
processing
fraud
notification
analytics
audit
```

## Standard Module Structure

```text
module-name

├── controller
├── service
├── repository
├── entity
├── dto
├── mapper
├── validator
├── event
└── exception
```

---

# 4. Database Migration Strategy

Migration Tool:

```text
Flyway
```

Database:

```text
claimlens_db
```

Schemas:

```text
public
processing
analytics
audit
```

## Migration Naming Convention

```text
V1__initial_schema.sql

V2__organization_tables.sql

V3__user_tables.sql

V4__policy_tables.sql

V5__claim_tables.sql

V6__document_tables.sql

V7__assignment_tables.sql

V8__investigation_tables.sql

V9__processing_tables.sql

V10__fraud_tables.sql

V11__notification_tables.sql

V12__analytics_tables.sql

V13__audit_tables.sql
```

## Rules

* Executed migrations must never be modified.
* New database changes require a new migration.
* Hibernate schema generation is prohibited.
* Flyway is the single source of truth.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

---

# 5. Environment Strategy

Supported Environments

```text
local
dev
qa
staging
prod
```

Configuration Files

```text
application.yml

application-local.yml
application-dev.yml
application-qa.yml
application-staging.yml
application-prod.yml
```

---

# 6. Environment Variables

## Database

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

## Redis

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
```

## AWS

```text
AWS_REGION
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

## S3

```text
S3_BUCKET_NAME
S3_DOCUMENT_PREFIX
```

## Security

```text
JWT_SECRET
JWT_EXPIRATION
JWT_REFRESH_EXPIRATION
```

## External Services

```text
OCR_SERVICE_URL
ANALYSIS_SERVICE_URL
```

## Monitoring

```text
PROMETHEUS_ENABLED
GRAFANA_ENABLED
```

---

# 7. Docker Setup

## Containers

```text
postgres
redis
backend
frontend
ocr-service
analysis-service
prometheus
grafana
```

## Startup

```bash
docker compose up -d
```

## Shutdown

```bash
docker compose down
```

---

# 8. Local Development Setup

## Required Software

```text
Java 21
Node.js 22+
Python 3.12+
Docker Desktop
Git
```

## Startup Sequence

```text
1. Start Docker Infrastructure

2. Run Flyway Migrations

3. Start Backend

4. Start OCR Service

5. Start Analysis Service

6. Start Frontend
```

---

# 9. Development Standards

## Java Standards

### Required

```text
Constructor Injection
Records for DTOs where appropriate
Immutable Objects preferred
```

### Prohibited

```text
Field Injection
Business Logic in Controllers
Direct Repository Access from Controllers
```

---

## Spring Standards

```text
Transactional Service Layer
Repository Pattern
Validation Layer
Centralized Exception Handling
Outbox Based Event Publishing
```

---

## Frontend Standards

```text
TypeScript Strict Mode
Feature Based Folder Structure
Material UI
RTK Query
React Hook Form
```

---

# 10. API Standards

## Response Format

Success

```json
{
  "success": true,
  "data": {}
}
```

Error

```json
{
  "success": false,
  "code": "CLAIM_NOT_FOUND",
  "message": "Claim not found",
  "traceId": "abc123"
}
```

---

# 11. Error Handling Strategy

## Exception Types

```text
ValidationException

NotFoundException

BusinessException

UnauthorizedException

ForbiddenException

ProcessingException
```

## Exception Handling

```text
GlobalExceptionHandler
```

All exceptions must be mapped to standard API responses.

---

# 12. Logging Standards

Framework

```text
SLF4J
Logback
```

Required MDC Fields

```text
tenantId
claimId
userId
traceId
requestId
```

Log Levels

```text
INFO
WARN
ERROR
```

Avoid DEBUG logging in production.

---

# 13. Security Standards

Authentication

```text
JWT Authentication
```

Authorization

```text
RBAC
```

Requirements

```text
Tenant Validation
Input Validation
Audit Logging
Password Encryption
Secure Secrets Management
```

---

# 14. Testing Strategy

## Unit Tests

Frameworks

```text
JUnit 5
Mockito
```

## Integration Tests

Framework

```text
Testcontainers
```

## API Tests

```text
Spring Boot Integration Tests
```

Coverage Target

```text
Minimum 70%
```

---

# 15. Observability Strategy

## Metrics

```text
Claim Processing Duration

OCR Processing Duration

Analysis Processing Duration

Fraud Evaluation Duration

Queue Backlog

Worker Failures

API Response Times

Database Query Times
```

## Monitoring Stack

```text
Prometheus
Grafana
```

---

# 16. Module Implementation Order

## Phase 1 Foundation

```text
Common
Security
Tenancy
Flyway Setup
Outbox Framework
```

## Phase 2 Core Business

```text
Organization
User
Policy
```

## Phase 3 Claims

```text
Claim
Document
```

## Phase 4 Operations

```text
Assignment
Investigation
```

## Phase 5 Processing

```text
OCR Pipeline
Analysis Pipeline
Processing Orchestrator
```

## Phase 6 Fraud

```text
Fraud Engine
```

## Phase 7 Platform

```text
Notification
Analytics
Audit
```

## Phase 8 Frontend

```text
Administration Screens
Claims Screens
Investigation Screens
Dashboard Screens
```

---

# 17. Sprint Plan

## Sprint 1

```text
Repository Setup
Flyway
Docker
Common Module
```

## Sprint 2

```text
Security
Authentication
Authorization
Tenant Context
```

## Sprint 3

```text
Organization Module
User Module
```

## Sprint 4

```text
Policy Module
```

## Sprint 5

```text
Claim Module
```

## Sprint 6

```text
Document Module
S3 Integration
```

## Sprint 7

```text
Assignment Module
Investigation Module
```

## Sprint 8

```text
OCR Pipeline
Analysis Pipeline
```

## Sprint 9

```text
Processing Orchestrator
Fraud Engine
```

## Sprint 10

```text
Notification
Analytics
Audit
Hardening
```

---

# 18. MVP Build Order

```text
Infrastructure

↓

Authentication

↓

Tenanting

↓

Organization

↓

Users

↓

Policies

↓

Claims

↓

Documents

↓

OCR

↓

Analysis

↓

Processing Orchestrator

↓

Fraud Engine

↓

Investigation

↓

Notifications

↓

Analytics

↓

Audit

↓

Frontend

↓

Production Readiness
```

---

# 19. Definition of Done

A module is considered complete only when:

* Database migration completed
* Entity model completed
* Repository completed
* Service layer completed
* API endpoints completed
* Validation completed
* Events implemented
* Unit tests implemented
* Integration tests implemented
* Audit logging implemented
* Documentation updated

---

# 20. Next Steps

After approval of this blueprint:

1. Create repository skeleton.
2. Configure Flyway.
3. Configure Docker environment.
4. Create Common module.
5. Create Security module.
6. Create Tenancy module.
7. Begin Organization module implementation.

This marks the transition from architecture design to active implementation.
