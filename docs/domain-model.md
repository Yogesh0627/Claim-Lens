# ClaimLens - Complete Domain Model

Status: Frozen
Version: 1.2
Owner: Niyo Technologies
Last Updated: June 2026

---

# Purpose

This document is the single source of truth for ClaimLens V1 domain modeling.
It combines:
- Business Domains
- Aggregate Boundaries
- Tenant Rules
- Immutability Rules
- Soft Delete Strategy
- Data Retention Strategy
- Final v1.2 Corrections

---

# Core Architecture Rules

## Tenant Rule

InsuranceCompany is the tenant root.

All business entities contain:

tenantId NOT NULL

Database equivalent:

tenant_id UUID NOT NULL REFERENCES insurance_company(id)

**As-built (2026-07-29):** primary keys and `tenant_id` are BIGINT identity (`Long`), not UUID. Tenancy is enforced at the ORM layer via Hibernate `@TenantId` on `TenantAwareEntity` (appended to every SELECT/UPDATE/DELETE including load-by-id), rather than by an application-supplied UUID. Aggregates that are exposed externally carry a separate opaque `public_id UUID` (e.g. `claim.public_id`) so the internal BIGINT is never leaked.

Exceptions:
- ServiceHealth
- SystemMetric
- AlertRule
- AlertEvent
- ErrorLog (nullable tenantId)

---

## Naming Convention

Domain Model:
- tenantId
- employeeCode
- createdAt

Database:
- tenant_id
- employee_code
- created_at

---

## Soft Delete Standard

Fields:
- isDeleted
- deletedAt
- deletedBy

Applied To:
- User
- Customer
- Region
- Branch
- DocumentType
- FraudRule
- NotificationTemplate

**As-built (2026-07-29):** the soft-delete columns (`is_deleted`, `deleted_at`, `deleted_by`) live on the shared `BaseEntity`, so every entity extending it inherits them — the list above is the set that actually exercises soft delete, not a schema restriction. Standalone append-only/infrastructure entities (`AuditLog`, `Notification`, `UserSession`) deliberately do NOT extend `BaseEntity` and have no soft-delete switch. Separately, a tenant (`InsuranceCompany`) can be retired via an `ARCHIVED` status (distinct from a soft delete) — see `InsuranceCompanyStatus`.

---

## Immutable Entities

Append Only:
- ClaimHistory
- AssignmentHistory
- OCRResult
- FraudScore
- AuditEvent
- AuditLog
- AnalyticsSnapshot

No Updates
No Deletes

**As-built (2026-07-29):** the realized append-only tables are `ClaimStatusHistory` (design "ClaimHistory"), `OcrResult`, `FraudScore`, and `AuditLog`. There is no separate `AuditEvent` entity — event + change detail collapsed into the single `AuditLog`. `AnalyticsSnapshot` and a persisted `AssignmentHistory` table were not built (see the respective domain notes below).

---

# Aggregate Boundaries

InsuranceCompany Aggregate
- InsuranceCompany
- Region
- Branch

Access Control Aggregate
- Role
- Permission
- RolePermission

User Aggregate
- User
- UserBranchAssignment

Policy Aggregate
- ClaimTypePolicy
- RequiredDocumentPolicy
- AssignmentPolicy
- FraudPolicy
- FraudRule
- SLAPolicy
- AnalysisStrategy

Claim Aggregate
- Claim
- ClaimHistory
- ClaimComment
- ClaimTag

Document Aggregate
- Document
- DocumentVersion

Investigation Aggregate
- Investigation
- InvestigationTask
- InvestigationFinding
- InvestigationEvidence
- InvestigationReport

Fraud Aggregate
- FraudCase
- FraudScore
- FraudAlert

Audit Aggregate
- AuditEvent
- AuditLog
- AuditAttachment

**As-built (2026-07-29):** the aggregate map above is the design intent; several members were consolidated or not built in V1. Key deltas (detailed in each domain section below):
- **Policy Aggregate** — realized as `ClaimType` plus a fraud `FraudRuleset` + `FraudRule` pair under the `ruleset` module. `ClaimTypePolicy`, `RequiredDocumentPolicy`, `AssignmentPolicy`, `FraudPolicy`, `SLAPolicy`, and `AnalysisStrategy` were not built as separate tables.
- **Claim Aggregate** — `Claim` + `ClaimStatusHistory` only; `ClaimComment` and `ClaimTag` were not built.
- **Investigation Aggregate** — collapsed to a single `InvestigationNote`. `Investigation`, `InvestigationTask`, `InvestigationFinding`, `InvestigationEvidence`, and `InvestigationReport` were not built (evidence is absorbed by `InvestigationNote.documentId`).
- **Fraud Aggregate** — realized as `FraudScore` (+ `FraudJob`, `FraudRuleset`/`FraudRule`). `FraudCase` and `FraudAlert` were **dropped / not built**.
- **Audit Aggregate** — a single `AuditLog`. `AuditEvent` and `AuditAttachment` were **dropped / not built**.

---

# 1. Organization Domain

InsuranceCompany
- id
- name
- code
- tenantKey
- status
- contactEmail
- contactPhone
- website
- headOfficeAddress
- logoUrl
- brandingConfig
- subscriptionPlan
- currency
- timezone
- createdAt
- updatedAt

Region
- id
- tenantId
- code
- name
- ownerUserId
- status
- description
- createdAt
- updatedAt

Branch
- id
- tenantId
- regionId
- code
- name
- ownerUserId
- email
- phone
- address
- city
- state
- country
- postalCode
- status
- description
- createdAt
- updatedAt

---

# 2. Access Control Domain

Role
- id
- code
- name
- description
- isSystemRole
- status
- createdAt
- updatedAt

Permission
- id
- code
- name
- module
- description
- createdAt

RolePermission
- id
- tenantId
- roleId
- permissionId
- createdAt

**As-built (2026-07-29):** Roles and permissions are global (not tenant-scoped); the base role→permission grants live in the global `role_permission` table. A per-tenant override table `tenant_role_permission` (migration V30) lets a tenant add/remove specific permissions on a role without forking the global role set. `Permission` is a seeded reference table rather than a mapped JPA aggregate. Resolved permissions are cached (Caffeine dev / Redis prod).

---

# 3. User Domain

User
- id
- tenantId
- roleId
- employeeCode
- firstName
- lastName
- email
- phoneNumber
- passwordHash
- regionId
- homeBranchId
- designation
- joiningDate
- status
- profilePictureUrl
- lastLoginAt
- isDeleted
- deletedAt
- deletedBy
- createdAt
- updatedAt

Customer
- id
- tenantId
- customerNumber
- firstName
- lastName
- email
- phoneNumber
- dateOfBirth
- address
- city
- state
- country
- postalCode
- status
- isDeleted
- deletedAt
- deletedBy
- createdAt
- updatedAt

UserBranchAssignment
- id
- tenantId
- userId
- branchId
- assignmentType
- status
- createdAt

**As-built (2026-07-29):** the mapped entity is `AppUser` (table `app_user`); `region_id` is present as designed. Auth/session state is split into two standalone (non-tenant-aware) entities added later: `UserSession` (V6/rotating refresh-token sessions, SHA-256 token hash at rest) and `UserInvitation` (V27, invite/reset tokens, SHA-256 at rest, single-use, TTL).

---

# 4. Policy Engine Domain

**As-built (2026-07-29):** of this domain, only `ClaimType` and the fraud rules were built. Fraud rules were named `FraudRuleset` + `FraudRule` (module `ruleset`, permission codes `RULESET_READ/WRITE`), **not** `FraudPolicy` + `FraudRule`. `FraudRuleset` holds `claimTypeId`, `name`, `mediumThreshold`, `highThreshold`, `status` (one ACTIVE ruleset per tenant+claim type); `FraudRule` holds the per-rule weight/parameters. `ClaimTypePolicy`, `DocumentType`, `RequiredDocumentPolicy`, `AssignmentPolicy`, `FraudPolicy`, `SLAPolicy`, and `AnalysisStrategy` were **not** built as separate tables in V1.

ClaimType
- id
- tenantId
- code
- name
- description
- isEnabled
- createdAt
- updatedAt

ClaimTypePolicy
- id
- tenantId
- claimTypeId
- name
- version
- status
- effectiveFrom
- effectiveTo
- createdBy
- createdAt
- updatedAt

DocumentType
- id
- tenantId
- code
- name
- description
- status
- isDeleted
- deletedAt
- deletedBy

RequiredDocumentPolicy
- id
- tenantId
- claimTypePolicyId
- documentTypeId
- isRequired
- minDocuments
- maxDocuments
- createdAt

AssignmentPolicy
- id
- tenantId
- claimTypePolicyId
- autoAssignmentEnabled
- assignmentStrategy
- maxActiveClaimsPerInvestigator
- allowCrossBranchAssignment
- allowCrossRegionAssignment
- createdAt

FraudPolicy
- id
- tenantId
- claimTypePolicyId
- highRiskThreshold
- mediumRiskThreshold
- autoEscalationThreshold
- createdAt

FraudRule
- id
- tenantId
- fraudPolicyId
- ruleCode
- ruleName
- weight
- ruleParameters
- isEnabled
- isDeleted
- deletedAt
- deletedBy
- createdAt

SLAPolicy
- id
- tenantId
- claimTypePolicyId
- riskLevel
- responseTimeHours
- resolutionTimeHours
- createdAt

AnalysisStrategy
- id
- tenantId
- claimTypePolicyId
- ocrEnabled
- imageAnalysisEnabled
- fraudAnalysisEnabled
- mlScoringEnabled
- manualReviewRequired
- createdAt

---

# 5. Claim Domain

Claim
- id
- tenantId
- claimNumber
- customerId
- claimTypeId
- claimTypePolicyId
- regionId
- branchId
- policyNumber
- vehicleRegistrationNumber
- incidentDate
- incidentAddress
- incidentLatitude
- incidentLongitude
- incidentDescription
- claimAmount
- approvedAmount
- status
- fraudScore
- riskLevel
- currentInvestigatorId
- submittedAt
- approvedAt
- rejectedAt
- reopenedAt
- closedAt
- lastStatusChangedAt
- createdBy
- createdAt
- updatedAt

**As-built (2026-07-29):** `Claim` carries `public_id` (opaque UUID), the pinned product lineage (`insurance_policy_id`, `insured_vehicle_id`, `insurance_product_id`, `insurance_product_version_id`, `claim_type_id`), write-once intake snapshots (`policy_number`, `vehicle_registration_number`), and `fraud_confirmed` (nullable ground-truth label set at decision time, migration V32, feeds fraud-model evaluation). It does not carry `claimTypePolicyId`, `riskLevel`, `fraudScore`, or `currentInvestigatorId` — risk/score live on `FraudScore`, and assignment lives on `ClaimAssignment`. `reopenedAt` is present (the `ClaimStatus` enum includes `REOPENED`).

ClaimHistory
- id
- tenantId
- claimId
- oldStatus
- newStatus
- changedByUserId
- reason
- metadata
- createdAt

ClaimComment
- id
- tenantId
- claimId
- userId
- comment
- isInternal
- createdAt
- updatedAt

ClaimTag
- id
- tenantId
- claimId
- tag
- createdBy
- createdAt

---

# 6. Document Domain

Document
- id
- tenantId
- claimId
- documentTypeId
- currentVersionId
- status
- uploadedBy
- createdAt
- updatedAt

DocumentVersion
- id
- tenantId
- documentId
- versionNumber
- fileName
- originalFileName
- fileExtension
- mimeType
- fileSize
- storagePath
- checksum
- uploadedBy
- uploadReason
- createdAt

---

# 7. Assignment Domain

**As-built (2026-07-29):** realized as a single `ClaimAssignment` entity. Separate `AssignmentHistory` and `AssignmentQueue` tables were not built; the processing pipeline uses the `processing` job tables plus `ClaimProcessingState` instead of a dedicated assignment queue.

Assignment
- id
- tenantId
- claimId
- assignedUserId
- assignedAt
- assignmentReason
- status
- createdAt
- updatedAt

AssignmentHistory
- id
- tenantId
- assignmentId
- oldUserId
- newUserId
- reason
- changedBy
- changedAt

AssignmentQueue
- id
- tenantId
- claimId
- priority
- queueReason
- createdAt
- status
- retryCount

---

# 8. Investigation Domain

**As-built (2026-07-29):** the entire domain collapsed to a single `InvestigationNote` entity (`claimId`, `noteType`, `note`, `severity`, optional `documentId` for evidence). `Investigation`, `InvestigationTask`, `InvestigationFinding`, `InvestigationEvidence`, and `InvestigationReport` were **not built** — `InvestigationFinding` and `InvestigationEvidence` were explicitly dropped.

Investigation
- id
- tenantId
- claimId
- investigatorId
- status
- startedAt
- completedAt
- summary
- createdAt
- updatedAt

InvestigationTask
- id
- tenantId
- investigationId
- title
- description
- status
- dueDate
- completedAt
- assignedTo

InvestigationFinding
- id
- tenantId
- investigationId
- category
- description
- severity
- findingDate

InvestigationEvidence
- id
- tenantId
- investigationId
- documentId
- documentVersionId
- evidenceType
- metadataSnapshot
- uploadedAt

InvestigationReport
- id
- tenantId
- investigationId
- reportNumber
- summary
- recommendation
- version
- generatedBy
- generatedAt

---

# 9. OCR Domain

**As-built (2026-07-29):** realized as `OcrJob` + `OcrResult` (module `processing`). There is no separate `OCRFieldExtraction` entity. Image analysis runs alongside OCR as `AnalysisJob` + `AnalysisResult`, and the whole pipeline is coordinated by `ProcessingJob`, `FraudJob`, and `ClaimProcessingState`.

OCRJob
- id
- tenantId
- documentVersionId
- status
- strategyId
- requestedAt
- startedAt
- completedAt
- failureReason
- createdAt
- updatedAt

OCRResult
- id
- tenantId
- ocrJobId
- rawText
- confidenceScore
- processingTimeMs
- language
- createdAt

OCRFieldExtraction
- id
- tenantId
- ocrResultId
- fieldName
- normalizedFieldKey
- fieldValue
- normalizedFieldValue
- confidenceScore
- validationStatus
- createdAt

---

# 10. Fraud Domain

**As-built (2026-07-29):** realized as `FraudScore` (weighted-rule score 0-100 → LOW/MEDIUM/HIGH, explainable), plus `FraudJob` (pipeline gate) and the `FraudRuleset`/`FraudRule` config (see Policy Engine note). `FraudCase` and `FraudAlert` were **dropped / not built**.

FraudCase
- id
- tenantId
- claimId
- investigationId
- detectionSource
- sourceReference
- status
- openedAt
- closedAt
- createdAt
- updatedAt

FraudScore
- id
- tenantId
- claimId
- score
- riskLevel
- ruleVersion
- scoredAt

FraudAlert
- id
- tenantId
- claimId
- fraudScoreId
- fraudRuleId
- severity
- message
- status
- metadataSnapshot
- createdAt
- resolvedAt
- resolvedBy

---

# 11. Notification Domain

**As-built (2026-07-29):** realized as a single `Notification` entity (`recipientUserId`, `type`, `title`, `message`, `isRead`, `createdAt`). Separate `NotificationTemplate` and `NotificationDelivery` tables were not built; outbound email is sent **synchronously** through the pluggable `EmailSender` provider (Resend/SMTP/Log), called in-process at each claim event — not via an outbox/event bus (the `outbox`/`events` packages are empty placeholders) and not from persisted template/delivery rows.

NotificationTemplate
- id
- tenantId
- name
- channel
- subject
- body
- isActive
- isDeleted
- deletedAt
- deletedBy

Notification
- id
- tenantId
- recipientUserId
- claimId
- templateId
- type
- idempotencyKey
- payload
- status
- readAt
- createdAt

NotificationDelivery
- id
- tenantId
- notificationId
- channel
- status
- providerResponse
- sentAt
- attemptCount
- lastAttemptAt

---

# 12. Analytics Domain

**As-built (2026-07-29):** none of these were built as persisted tables in V1. The `analytics` module computes metrics on read (guarded by `ANALYTICS_READ`); `ReportDefinition`, `ReportExecution`, `DashboardMetric`, and `AnalyticsSnapshot` do not exist as entities.

ReportDefinition
- id
- tenantId
- name
- description
- reportType
- filters
- createdBy
- isActive
- createdAt
- updatedAt

ReportExecution
- id
- tenantId
- reportDefinitionId
- requestedBy
- status
- resultLocation
- startedAt
- completedAt
- createdAt

DashboardMetric
- id
- tenantId
- metricName
- metricValue
- metricDimension
- metricDate
- calculatedAt

AnalyticsSnapshot
- id
- tenantId
- snapshotType
- snapshotDate
- data
- generatedAt

---

# 13. Audit Domain

**As-built (2026-07-29):** realized as a single append-only `AuditLog` (`tenantId`, `userId`, `action`, `entityType`, `entityId`, `details`, `createdAt`), written from an audit aspect. `AuditEvent` and `AuditAttachment` were **dropped / not built** — event and change detail collapsed into `AuditLog`.

AuditEvent
- id
- tenantId
- eventType
- entityType
- entityId
- performedBy
- occurredAt

AuditLog
- id
- tenantId
- auditEventId
- oldValue
- newValue
- changeSummary
- createdAt

AuditAttachment
- id
- tenantId
- auditEventId
- documentId
- metadataSnapshot
- createdAt

---

# 14. Observability Domain

**As-built (2026-07-29):** not modeled as database tables. `ServiceHealth`, `SystemMetric`, `ErrorLog`, `AlertRule`, and `AlertEvent` do not exist as entities; health is a live endpoint (`HealthController`) and observability is handled operationally (logs / platform tooling), not persisted domain rows.

ServiceHealth
- id
- serviceName
- instanceId
- status
- checkedAt

SystemMetric
- id
- serviceName
- metricName
- metricValue
- metricUnit
- capturedAt

ErrorLog
- id
- tenantId (nullable)
- serviceName
- errorCode
- errorMessage
- stackTrace
- severity
- occurredAt

AlertRule
- id
- serviceName
- metricName
- operator
- threshold
- isActive

AlertEvent
- id
- alertRuleId
- status
- message
- triggeredAt
- resolvedAt

---

# Data Retention Strategy

Soft Delete:
- User
- Customer
- Region
- Branch
- DocumentType
- FraudRule
- NotificationTemplate

Status Lifecycle:
- Claim
- Investigation
- FraudCase
- Assignment

**As-built (2026-07-29):** the status-lifecycle entities that shipped are `Claim` and `ClaimAssignment`; `Investigation` and `FraudCase` were not built (see domain notes). Because the soft-delete columns sit on `BaseEntity`, soft delete is available to every business aggregate, not only the entities named above.

Permanent Records:
- AuditEvent
- AuditLog
- OCRResult
- FraudScore
- AssignmentHistory
- ClaimHistory

---

# Status

ClaimLens Domain Model v1.2
Status: Frozen

Ready For:
- Logical Database Schema Design
- Physical Database Design
- Index Strategy
- Partition Strategy
- Redis Design
- S3 Structure
- Service Boundaries
- API Contracts
- High Level Design

**As-built (2026-07-29):** list APIs use a shared envelope `PagedResponse<T>` `{content, page, size, totalElements, totalPages, first, last}` (helper `PageRequests` clamps page ≥ 0 and size to 1..100). Paginated endpoints: `GET /claims`, `/customers`, `/users`, `/policies` (`?page&size`), each with a sibling unpaged `/options` endpoint for pickers/dropdowns.



# Insurance Product Domain

Status: Approved

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

The Insurance Product Domain manages:

* Insurance Products
* Product Versioning
* Policy Documents
* Coverage Knowledge
* Product-Specific Rules

This domain acts as the bridge between:

```text
Insurance Company
        ↓
Insurance Product
        ↓
Claim
        ↓
Investigation
```

and enables:

* Policy Intelligence
* Coverage Validation
* Policy RAG
* Investigation Copilot
* Product-Specific Business Rules

---

# Why This Domain Exists

A Claim Type is not enough.

Example:

```text
Claim Type

MOTOR
```

Products:

```text
Private Car Premium

Private Car Basic

Commercial Vehicle Gold

Commercial Vehicle Standard
```

All belong to:

```text
MOTOR
```

But each product contains:

```text
Different Coverage

Different Exclusions

Different Policy Documents

Different Claim Rules

Different Fraud Rules
```

Therefore:

```text
Claim Type
```

and

```text
Insurance Product
```

must be modeled separately.

---

# Domain Structure

Insurance Company

↓

Insurance Product

↓

Insurance Product Version

↓

Product Documents

↓

Knowledge Base

---

# Entities

## InsuranceProduct

Represents an insurance product offered by a company.

Examples:

```text
Private Car Premium

Private Car Basic

Commercial Vehicle Gold
```

Responsibilities:

```text
Product Identity

Product Lifecycle

Product Configuration
```

Relationships:

```text
InsuranceCompany
        ↓
InsuranceProduct
```

---

## InsuranceProductVersion

Represents a version of an insurance product.

Purpose:

Insurance policies change over time.

Claims must always remain linked to the exact version active when the claim was created.

Examples:

```text
Private Car Premium V1

Private Car Premium V2

Private Car Premium V3
```

Relationships:

```text
InsuranceProduct
        ↓
InsuranceProductVersion
```

---

## ProductDocument

Represents official documents attached to a product version.

Examples:

```text
Policy Wording

Coverage Guide

Claim Manual

Fraud SOP

Underwriting Guide
```

Relationships:

```text
InsuranceProductVersion
        ↓
ProductDocument
```

---

## ProductKnowledgeBase

Represents searchable knowledge generated from policy documents.

Purpose:

Supports:

```text
Policy Copilot

Coverage Validation

Investigation Copilot

Scenario-Based Questions
```

Relationships:

```text
ProductDocument
        ↓
ProductKnowledgeBase
```

**As-built (2026-07-29):** the "ProductKnowledgeBase" concept was realized as three concrete entities in the `coverage` module: `PolicyChunk` (embedded, searchable chunks of a product version's documents — pgvector HNSW when enabled, else in-Java cosine), plus `CoverageAnswer` and `CoverageCitation` which log each RAG coverage Q&A and its source citations. `ProductDocument` and `DocumentVersion` were built as designed. There is no single entity literally named `ProductKnowledgeBase`.

---

# Product Versioning

Example

Product

```text
Private Car Premium
```

Version History

```text
V1
Effective:
01-Jan-2026

V2
Effective:
01-Jul-2026

V3
Effective:
01-Jan-2027
```

Claims created under:

```text
V1
```

must always remain associated with:

```text
V1
```

even after:

```text
V2

V3
```

are released.

---

# Claim Domain Relationship

Current

```text
Claim
    ↓
ClaimType
```

Updated

```text
Claim
    ↓
InsuranceProduct
    ↓
InsuranceProductVersion
    ↓
ClaimType
```

---

# Claim Changes

The Claim Aggregate must store:

```text
insuranceProductId

insuranceProductVersionId
```

Purpose:

Guarantees:

```text
Policy Traceability

Coverage Accuracy

Historical Consistency
```

---

# Knowledge & RAG Integration

Policy Documents

↓

OCR

↓

Chunking

↓

Embeddings

↓

Vector Database

↓

Knowledge Base

↓

LLM

↓

Policy Copilot

---

# Example Use Cases

Coverage Validation

```text
Is flood damage covered?
```

---

Policy Question

```text
Does engine protection cover water ingress?
```

---

Scenario Question

```text
My son was driving the vehicle and had an accident.

Will this claim be covered?
```

---

Investigation Question

```text
Does this claim violate any policy exclusions?
```

---

# Domain Relationships

InsuranceCompany

↓

InsuranceProduct

↓

InsuranceProductVersion

↓

ProductDocument

↓

ProductKnowledgeBase

---

Claim

↓

InsuranceProduct

↓

InsuranceProductVersion

---

ClaimType

↓

InsuranceProduct

---

# Future Extensions

V2

```text
Coverage Rules

Exclusion Rules

Benefit Catalog

Premium Configuration

Underwriting Rules
```

These may either be:

```text
Explicitly Modeled
```

or

```text
Derived From Policy Documents Through RAG
```

depending on product requirements.

---

# Domain Summary

Entities

```text
InsuranceProduct

InsuranceProductVersion

ProductDocument

ProductKnowledgeBase
```

Supports

```text
Product Management

Policy Versioning

Coverage Validation

Policy Intelligence

RAG

Investigation Copilot
```

Enterprise Insurance Modeling

```text
YES
```
