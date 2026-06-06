# ClaimLens - Complete Domain Model

Status: Frozen
Version: 1.2
Owner: RiskLens Technologies
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

---

# 4. Policy Engine Domain

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
