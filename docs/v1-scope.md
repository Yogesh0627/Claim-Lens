# ClaimLens - Version 1 Scope

Status: Draft

Version: 1.0

Owner: RiskLens Technologies

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
* UNDER_INVESTIGATION
* WAITING_FOR_CUSTOMER
* APPROVED
* REJECTED
* CLOSED
* REOPENED

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

## Not Included

* AI Document Understanding
* LLM-Based Document Analysis

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

---

# Deployment

## Included

* AWS Deployment
* Dockerized Services
* S3 Document Storage

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
