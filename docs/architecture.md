# ClaimLens - Architecture

Status: Frozen

Version: 1.0

Owner: Niyo Technologies

Last Updated: June 2026

---

# Purpose

This document defines the complete architecture of ClaimLens V1.

It covers:

* System Components
* Runtime Processing
* Service Communication
* Infrastructure Design
* Deployment Strategy
* Scalability Strategy
* Monitoring Strategy
* Failure Recovery

---

# Architecture Principles

ClaimLens follows:

1. Multi-Tenant SaaS Architecture
2. Modular Monolith Core
3. Service-Oriented Processing
4. Event-Driven Background Processing
5. API First Design
6. Auditability By Default
7. Horizontal Scalability

---

# Architecture Style

Core Platform

* Spring Boot Modular Monolith

Processing Components

* OCR Service
* Analysis Service

Storage Components

* PostgreSQL
* Redis
* S3

Observability Components

* Prometheus
* Grafana

Reason:

Business workflows remain centralized while computational workloads scale independently.

---

# System Components

1. Frontend
2. Backend API
3. Claim Processing Orchestrator
4. OCR Service
5. Analysis Service
6. PostgreSQL
7. Redis
8. S3
9. Prometheus
10. Grafana

---

# Frontend

Technology

* Next.js
* TypeScript
* Material UI

Responsibilities

* Customer Portal
* Employee Portal
* Investigator Portal
* Manager Portal
* Admin Portal
* Dashboards
* Notifications

Scaling

Horizontal

---

# Backend API

Technology

* Spring Boot

Responsibilities

* Authentication
* Authorization
* Tenant Isolation
* Claim Management
* Document Management
* Assignment Engine
* Investigation Workflow
* Fraud Rule Execution
* Notification Orchestration
* Audit Logging

The backend acts as the system orchestrator.

---

# Claim Processing Orchestrator

Purpose

Coordinate claim processing stages safely.

Responsibilities

* Track OCR Completion
* Track Analysis Completion
* Queue Fraud Jobs
* Prevent Duplicate Execution
* Manage Reprocessing
* Recover Failed Jobs

Implemented inside Spring Boot.

Separate service not required in V1.

---

# OCR Service

Technology

* Python
* EasyOCR
* Tesseract

Responsibilities

* OCR Processing
* Text Extraction
* Structured Data Extraction

Scales independently.

---

# Analysis Service

Technology

* Python
* OpenCV

Responsibilities

* Duplicate Image Detection
* Similarity Detection
* Metadata Analysis
* Fraud Signal Generation

Scales independently.

---

# Communication Strategy

Two communication models:

Synchronous

Used for:

* Login
* Claim Creation
* Claim Submission
* Assignment Acceptance
* Investigation Updates

Asynchronous

Used for:

* OCR
* Image Analysis
* Fraud Analysis
* Notification Delivery
* Analytics Aggregation

---

# Claim Submission Flow

Customer
↓
Submit Claim
↓
Backend Validation
↓
Generate Claim Number
↓
Store Claim
↓
Store Documents
↓
Create OCR Jobs
↓
Create Analysis Jobs
↓
Create ClaimProcessingState
↓
Status = AWAITING_ANALYSIS
↓
Return Success

Customer never waits for OCR or Fraud Analysis.

---

# Claim Processing State

Table:

claim_processing_state

Fields

* claim_id
* ocr_status
* analysis_status
* fraud_status
* pending_reprocess
* last_updated_at

Purpose

Track processing lifecycle.

---

# OCR Flow

Worker Claims Job
↓
FOR UPDATE SKIP LOCKED
↓
Download Document Version
↓
OCR Processing
↓
Store OCR Metadata
↓
Store OCR Field Extractions
↓
Update ClaimProcessingState
↓
OCR Complete

---

# Analysis Flow

Worker Claims Job
↓
FOR UPDATE SKIP LOCKED
↓
Download Images
↓
OpenCV Processing
↓
Duplicate Detection
↓
Similarity Detection
↓
Metadata Extraction
↓
Store Results
↓
Update ClaimProcessingState
↓
Analysis Complete

---

# Fraud Trigger Strategy

Only Orchestrator can queue Fraud Jobs.

OCR and Analysis services never trigger Fraud directly.

Atomic Update:

Fraud queued only when:

* OCR Complete
* Analysis Complete
* Fraud Not Started

Result:

Exactly One Fraud Job Per Claim

No Race Conditions

No Duplicate Execution

---

# Claim Snapshot Builder

Purpose

Build current claim state before fraud execution.

Example:

Doc1 → V1

Doc2 → V1

Doc3 → V1

Doc4 → V1

Doc5 → V2

↓

Current Claim Snapshot

↓

Fraud Engine

This avoids reprocessing unchanged documents.

---

# Fraud Analysis Flow

Load Fraud Policy
↓
Load Fraud Rules
↓
Build Claim Snapshot
↓
Execute Rules
↓
Generate Fraud Score
↓
Generate Fraud Alerts
↓
Update Claim
↓
Queue Assignment

---

# Assignment Flow

Load Assignment Policy
↓
Find Eligible Investigators

Filters:

* Same Tenant
* Active
* Correct Region
* Correct Branch
* Not On Leave

↓

Apply Strategy

* Round Robin
* Least Loaded

↓

Create Assignment
↓
Notify Investigator
↓
Status = AWAITING_ACCEPTANCE

---

# Assignment Acceptance

Investigator
↓
Accept Assignment
↓
Create Investigation
↓
Status = UNDER_INVESTIGATION

---

# Assignment Rejection

Investigator
↓
Reject Assignment
↓
Assignment History
↓
Reassignment Triggered
↓
New Assignment

---

# Additional Information Flow

Investigator
↓
Request Documents
↓
WAITING_FOR_CUSTOMER

Customer
↓
Upload New Version
↓
DocumentVersion Created

Only new document versions are processed.

---

# Incremental Reprocessing

Example

Claim contains:

Doc1 V1
Doc2 V1
Doc3 V1
Doc4 V1
Doc5 V1

Customer uploads:

Doc5 V2

Processing:

OCR(Doc5 V2)

Analysis(Doc5 V2)

Rebuild Snapshot

Fraud Recalculation

No processing of Docs 1–4.

---

# Reprocessing Protection

ClaimProcessingState contains:

pending_reprocess

If processing already running:

pending_reprocess = TRUE

No duplicate processing jobs created.

When active processing completes:

One reprocessing cycle executes.

---

# Upload Guardrails

Before OCR:

Validate

* File Type
* File Size
* Page Count
* Corruption Check

Reject unsupported files before worker consumption.

---

# Tenant Rate Limiting

Redis

Examples

tenant:uploads_per_minute

tenant:ocr_requests_per_hour

tenant:analysis_requests_per_hour

Protects platform stability.

---

# PostgreSQL Architecture

Database

claimlens_db

Strategy

Single Shared Database

Tenant Isolation

tenant_id

All business entities contain:

tenant_id

PostgreSQL remains the source of truth.

---

# Database Schemas

public

* claim
* assignment
* investigation
* customer
* user

processing

* ocr_job
* analysis_job
* fraud_job
* notification_job
* claim_processing_state
* ocr_result

audit

* audit_event
* audit_log

analytics

* analytics_snapshot
* dashboard_metric

---

# Job Processing Strategy

Job Tables

* ocr_job
* analysis_job
* fraud_job
* notification_job

Worker Claims Job

Using:

FOR UPDATE SKIP LOCKED

Benefits

* No Duplicate Processing
* Safe Parallel Execution

---

# Worker Crash Recovery

Job Status

* PENDING
* PROCESSING
* COMPLETE
* FAILED

Fields

* locked_at
* attempt_count

Recovery Worker

Find:

PROCESSING

Older Than Threshold

↓

Reset To PENDING

↓

Retry

No Lost Jobs.

---

# Redis Architecture

Used For

* Caching
* Rate Limiting
* Assignment Locks
* Dashboard Caching

Redis is never a source of truth.

---

# S3 Architecture

Stores

* Documents
* Images
* Evidence
* Reports
* OCR Payloads

Database stores metadata only.

---

# OCR Storage Strategy

Large OCR payloads are not stored inside PostgreSQL.

Example:

ocr-result.json

↓

S3

Database stores:

* payloadLocation
* confidenceScore
* processingTimeMs

Benefits

* Lower Disk IO
* Faster Queries
* Smaller Tables

---

# Monitoring Architecture

Micrometer
↓
Prometheus
↓
Grafana

Metrics

* API Latency
* Request Count
* Error Rate
* JVM Metrics
* DB Pool Metrics
* OCR Processing Time
* Analysis Processing Time

---

# Logging Architecture

Structured JSON Logs

Fields

* timestamp
* traceId
* requestId
* tenantId
* userId
* serviceName

Benefits

* Traceability
* Debugging
* Audit Support

---

# Health Checks

Every Service Exposes

/health

Checks

* PostgreSQL
* Redis
* S3

Used by:

* Monitoring
* Load Balancer

---

# Failure Recovery

OCR Failure
↓
Retry
↓
Manual Review Queue

Analysis Failure
↓
Retry
↓
Manual Review Queue

Notification Failure
↓
Retry
↓
Dead Letter Queue

---

# Scalability Strategy

Frontend

Horizontal Scaling

Backend

Horizontal Scaling

OCR Service

Independent Scaling

Analysis Service

Independent Scaling

Database

Vertical Scaling Initially

Read Replicas Later

Redis

Single Instance V1

Cluster Future

---

# Security Architecture

* JWT Authentication
* RBAC
* Tenant Isolation
* Password Hashing
* HTTPS
* Audit Logging
* Secure S3 Access

---

# Deployment Target

AWS

Services

* EC2
* RDS PostgreSQL
* ElastiCache Redis
* S3
* Application Load Balancer

Deployment

Docker Containers On EC2

No Kubernetes In V1

---

# Future Evolution

V2

* RabbitMQ
* Kafka
* Read Replicas
* Advanced Fraud AI

V3

* Kubernetes
* Multi Region Deployment
* Event Streaming
* ML Fraud Models

---

# Architecture Summary

Users
↓
Load Balancer
↓
Frontend
↓
Backend API
↓
Claim Processing Orchestrator

Backend
↓
PostgreSQL

Backend
↓
Redis

Backend
↓
S3

Orchestrator
↓
OCR Workers

Orchestrator
↓
Analysis Workers

Orchestrator
↓
Fraud Workers

Prometheus
↓
Grafana

ClaimLens V1 is designed as a scalable, multi-tenant, production-ready insurance claim investigation platform with strong auditability, fault tolerance, and operational observability.
