# ClaimLens - Version 1 Scope

Status: Draft

Version: 1.0

Owner: Niyo Technologies

Last Updated: June 2026

---

# Purpose

This document defines the exact scope of ClaimLens Version 1 (V1).

The goal is to build a production-ready, enterprise-grade claims investigation platform focused on Motor Insurance Claims while avoiding unnecessary complexity that delays delivery.

---

# V1 Goal

Deliver a fully functional SaaS platform that enables insurance companies to:

* Manage claims
* Manage documents
* Perform OCR extraction
* Perform image analysis
* Generate fraud risk scores
* Assign investigators automatically
* Conduct investigations
* Track claim lifecycle
* Monitor operational performance
* Maintain complete audit trails

---

# Target Industry

Insurance Industry

---

# Supported Claim Types

## Included

* Motor Insurance Claims

## Not Included

* Health Insurance Claims
* Property Insurance Claims
* Travel Insurance Claims
* Life Insurance Claims

The architecture will support future claim types, but only Motor Claims will be operational in V1.

---

# SaaS Model

## Included

* Multi-Tenant Architecture
* Tenant Isolation
* Company-Level Branding
* Company-Level Configuration

---

# User Roles

## Included

### Company Admin

Responsibilities:

* Manage company configuration
* Manage users
* Manage policies
* Access analytics
* Access audit logs

### Regional Admin

Responsibilities:

* Manage region operations
* Monitor branch performance
* View regional analytics

### Investigation Manager

Responsibilities:

* Monitor investigations
* Reassign investigators
* Manage investigation workload

### Investigator

Responsibilities:

* Review assigned claims
* Request additional information
* Add investigation findings
* Approve or reject claims

### Employee

Responsibilities:

* Create claims on behalf of customers
* Assist claim processing

### Customer

Responsibilities:

* Create claims
* Upload documents
* Respond to information requests
* Track claim status

**As-built (2026-07-29):** V1 shipped a richer, finer-grained set of **11 global roles** (RBAC is permission-based, not role-name-based). The roles above map onto these codes: TENANT_ADMIN (Company Admin), PRODUCT_MANAGER, CLAIMS_MANAGER, INVESTIGATION_MANAGER, INVESTIGATOR, CLAIMS_ADJUSTER, CUSTOMER_SUPPORT, AUDITOR, ANALYST, PLATFORM_ADMIN (platform operator, global), and CUSTOMER. "Regional Admin" and "Employee" were not shipped as distinct roles; regional scoping is handled via region/branch assignment and the manager roles.

---

# Claim Management

## Included

* Draft Claims
* Claim Submission
* Claim Tracking
* Claim Reopening
* Claim Status History
* Claim Comments
* Claim Tags

**As-built (2026-07-29):** Drafts, submission, tracking, and full status history shipped. **Claim reopening is only partial** — `REOPENED` exists as a status (enum + DB constraint + `reopened_at` column) but no transition is wired to it (no reopen endpoint/service method), so a claim cannot actually be reopened in V1; the closest live behaviour is the info-request loop (WAITING_FOR_CUSTOMER → UNDER_INVESTIGATION). **Claim Comments and Claim Tags were not built** — investigator annotation is served by structured Investigation Notes instead.

## Not Included

* Bulk Claim Uploads
* Claim Merging
* Cross-Tenant Claims

---

# Claim Lifecycle

Supported States:

* DRAFT
* SUBMITTED
* AWAITING_ANALYSIS
* AWAITING_ASSIGNMENT
* AWAITING_ACCEPTANCE
* UNDER_INVESTIGATION
* WAITING_FOR_CUSTOMER
* APPROVED
* REJECTED
* CLOSED
* REOPENED

**As-built (2026-07-29):** The `ClaimStatus` enum ships all 11 states above (AWAITING_ACCEPTANCE covers the assignment-acceptance handshake). Terminal states are APPROVED, REJECTED, CLOSED.

---

# Document Management

## Included

* Document Upload
* Document Versioning
* Document Categorization
* Document Validation
* File Integrity Checks
* Document Audit History

Supported Examples:

* Accident Photos
* Police Report
* Repair Estimate
* Vehicle Registration Certificate

## Not Included

* Video Analysis
* Audio Analysis
* Real-Time Media Processing

---

# OCR Processing

## Included

* OCR Job Processing
* Text Extraction
* Structured Data Extraction
* OCR Result Storage

Technology:

* Python
* EasyOCR / Tesseract

**As-built (2026-07-29):** OCR runs behind an interface with an offline default; the shipped providers are **Google Vision** (`OCR_PROVIDER=vision`) or a **Python Tesseract service** (`=http`), selectable per environment. OCR jobs run per-document in the async processing pipeline.

## Not Included

* AI Document Understanding
* LLM-Based Document Analysis (per-document OCR only; note that a separate LLM-based RAG coverage assistant — see below — was added for policy Q&A)

---

# Image Analysis

## Included

* Duplicate Image Detection
* Similar Image Detection
* Image Metadata Analysis

Technology:

* OpenCV

## Not Included

* Deep Learning Vision Models
* Facial Recognition
* Vehicle Damage Assessment AI

---

# Fraud Analysis

## Included

* Rule-Based Fraud Scoring
* Fraud Risk Categories
* Fraud Rules Configuration
* Fraud Explanations

Risk Levels:

* LOW
* MEDIUM
* HIGH

Examples:

* Duplicate Image Detection
* Duplicate Claim Detection
* Missing Required Documents
* Location Mismatch
* Repair Cost Anomaly

**As-built (2026-07-29):** Fraud scoring is a weighted, explainable rule engine (`FraudEngine.evaluate()` → 0-100 → LOW/MEDIUM/HIGH), with per-rule contributions persisted for explainability. A fraud-evaluation harness ships at `tools/fraud-eval` for regression-testing the ruleset.

## Not Included

* Machine Learning Fraud Models
* Graph-Based Fraud Networks
* Predictive Fraud AI

---

# Assignment Engine

## Included

* Auto Assignment
* Manual Reassignment
* Workload-Based Assignment
* Assignment History
* Assignment Audit Trail

Supported Strategies:

* Round Robin
* Least Loaded
* Manual Assignment

---

# Investigation Workflow

## Included

* Investigation Notes
* Findings Recording
* Information Requests
* Investigation Decisions
* Claim Approval
* Claim Rejection

---

# Policy Engine

## Included

* Claim Type Policies
* Required Document Policies
* Assignment Policies
* Fraud Policies
* SLA Policies
* Analysis Strategies

Company Admins can configure policies without code changes.

---

# Notifications

## Included

* Assignment Notifications
* Status Change Notifications
* Document Request Notifications
* Investigation Updates

Delivery Channels:

* In-App Notifications
* Email Notifications

**As-built (2026-07-29):** Email delivers through a provider interface (offline log default); production uses **Resend** (`EMAIL_PROVIDER`). Invitation and password-reset emails are part of the shipped onboarding flow.

## Not Included

* SMS Notifications
* WhatsApp Notifications
* Push Notifications

---

# Analytics

## Included

### Claim Analytics

* Claims by Status
* Claims by Region
* Claims by Branch

### Fraud Analytics

* Fraud Distribution
* High Risk Claims
* Fraud Rule Effectiveness

### Investigator Analytics

* Active Workload
* Average Resolution Time
* Claims Closed

---

# Audit & Compliance

## Included

* User Activity Logs
* Claim History
* Investigation History
* Assignment History
* Document History

---

# Observability

## Included

### Metrics

* API Latency
* Request Rate
* Error Rate
* Database Connection Pool Health
* OCR Processing Time
* Fraud Analysis Time

### Monitoring Stack

* Micrometer
* Prometheus
* Grafana

### Logging

* Structured JSON Logging
* Correlation IDs
* Trace IDs

---

# Infrastructure

## Included

### Containers

* Frontend Container
* Backend Container
* OCR Service Container
* Analysis Service Container
* PostgreSQL Container
* Redis Container

Technology:

* Docker

---

# Security

## Included

* JWT Authentication
* Role-Based Access Control
* Tenant Isolation
* Password Encryption
* Audit Logging

**As-built (2026-07-29):** Also shipped: server-side per-request permission resolution (JWT carries roleId, not permissions), SHA-256-hashed single-use rotating refresh/invite/reset tokens, and **request rate limiting** (auth / AI / upload buckets, Redis in prod). A dedicated security-hardening pass was completed — stored-XSS on document download fixed, proper 405/415/413 handlers, explicit upload size limits, `/roles` authz — with remaining known gaps documented. Full detail in `docs/audit-report.md`. Backend ships **106 passing tests** (3 skipped) run against a real PostgreSQL test database.

---

# Deployment

## Included

* AWS Deployment
* Dockerized Services
* S3 Document Storage

**As-built (2026-07-29):** The delivered demo deployment targets **Render** (backend + FastAPI analysis service, Dockerized) and **Vercel** (frontend), with **Neon** PostgreSQL, **Upstash** Redis (cache + rate limit), and **Cloudflare R2** for document storage (S3-compatible; storage is behind a provider interface with a local-filesystem default). AWS remains a supported target but was not the shipped host.

---

# Out of Scope for V1

The following features are intentionally excluded:

* Advanced Machine Learning Models
* Fraud Prediction AI
* Video Analysis
* Mobile Applications
* Graph Databases
* Real-Time Streaming Analytics
* External Insurance System Integrations
* Multi-Region Deployment
* Event-Driven Microservices
* Kubernetes
* Advanced Workflow Builder
* AI Investigation Assistant

**As-built (2026-07-29):** A full AI *investigation* assistant remains out of scope, but V1 did ship an **LLM-based RAG coverage assistant** for policy Q&A (Gemini for chat + embeddings, pgvector retrieval, answers with citations; offline stub default). This is a coverage-explanation aid, not an autonomous investigation agent.

---

# As-Built Additions (2026-07-29)

Capabilities delivered in V1 that were not called out in the original scope draft:

* **Customer portal** — a customer-facing surface (second ownership gate) for creating claims, uploading documents, responding to information requests, and tracking status.
* **RAG coverage assistant** — Gemini-backed policy Q&A with citations and pgvector retrieval (offline stub fallback).
* **Full org hierarchy** — region, branch, department, designation, plus reporting chains and user region/branch assignment.
* **Product catalog + versioning + policies** — insurance products with immutable versions and per-product required-document policies; policy documents chunked for the coverage assistant.
* **Document versioning** with safe-download handling (content-type allowlist).
* **Information-request loop** — the WAITING_FOR_CUSTOMER cycle for requesting and receiving additional info.
* **Platform console + impersonation** — a global platform-admin surface for cross-tenant operations and tenant impersonation.
* **Invitation-based onboarding** — token-based user invitations and password reset (emails via Resend).
* **Request rate limiting** — auth / AI / upload buckets (Redis in prod, in-memory in dev).
* **Server-side pagination** — paged list endpoints (`/claims`, `/customers`, `/users`, `/policies`) with unpaged `/options` siblings for pickers; frontend pagination hook + bar.
* **Fraud-evaluation harness** — `tools/fraud-eval` for regression-testing the fraud ruleset.

---

# Definition of V1 Success

ClaimLens V1 is considered successful when:

1. A customer can create and submit a motor insurance claim.
2. Required documents can be uploaded and versioned.
3. OCR and image analysis execute successfully.
4. Fraud scoring is generated.
5. Claims are automatically assigned.
6. Investigators can review and make decisions.
7. Analytics are generated.
8. Audit trails are maintained.
9. Observability dashboards display system health.
10. The platform can support multiple insurance companies safely and independently.
