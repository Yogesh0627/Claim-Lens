# ClaimLens - Business Flows

Status: Draft

Version: 1.0

Owner: Niyo Technologies

Last Updated: June 2026

---

# Purpose

This document defines the end-to-end business workflows of ClaimLens V1.

The workflows described here represent how users, investigators, managers, and system services interact throughout the claim lifecycle.

---

# Flow 1 - Insurance Company Onboarding

## Objective

Provision a new insurance company tenant within ClaimLens.

## Flow

Platform Admin
↓
Create Insurance Company
↓
Generate Tenant Configuration
↓
Configure Branding
↓
Create Company Admin
↓
Install Default Motor Claim Policies
↓
Activate Tenant
↓
Company Ready

## Outcome

A fully isolated tenant is available for use.

---

# Flow 2 - Organization Setup

## Objective

Configure the company structure.

## Flow

Company Admin
↓
Create Regions
↓
Create Branches
↓
Create Users
↓
Assign Roles
↓
Configure Policies
↓
Organization Ready

## Outcome

Operational hierarchy is established.

---

# Flow 3 - Customer Registration

## Objective

Register a customer who can submit claims.

## Flow

Customer
↓
Create Account
↓
Verify Email
↓
Profile Created

OR

Employee
↓
Register Customer
↓
Customer Profile Created

## Outcome

Customer becomes eligible to submit claims.

---

# Flow 4 - Claim Creation

## Objective

Allow customers to create motor insurance claims.

## Flow

Customer
↓
Create Draft Claim
↓
Enter Incident Details
↓
Enter Vehicle Information
↓
Enter Policy Information
↓
Upload Documents
↓
Save Draft

OR

Employee
↓
Create Claim On Behalf Of Customer

## Outcome

Draft claim exists.

---

# Flow 5 - Claim Submission

## Objective

Submit a claim for processing.

## Flow

Customer
↓
Submit Claim
↓
Validate Required Documents
↓
Validate Mandatory Fields
↓
Generate Claim Number
↓
Claim Status = SUBMITTED
↓
Create Audit Record

## Outcome

Claim enters processing pipeline.

---

# Flow 6 - OCR Processing

## Objective

Extract information from uploaded documents.

## Flow

Claim Submitted
↓
OCR Job Created
↓
OCR Service Receives Request
↓
Process Document Versions
↓
Extract Text
↓
Extract Structured Fields
↓
Store OCR Results
↓
Update Processing Status

## Outcome

OCR data becomes available for investigation and fraud analysis.

---

# Flow 7 - Image Analysis

## Objective

Analyze uploaded images.

## Flow

Claim Submitted
↓
Image Analysis Job Created
↓
OpenCV Processing
↓
Duplicate Image Detection
↓
Image Similarity Analysis
↓
Metadata Extraction
↓
Store Analysis Results

## Outcome

Image insights become available.

---

# Flow 8 - Fraud Analysis

## Objective

Generate a fraud risk score.

## Flow

OCR Completed
↓
Image Analysis Completed
↓
Load Fraud Policy
↓
Execute Fraud Rules
↓
Calculate Fraud Score
↓
Determine Risk Level
↓
Generate Fraud Reasons
↓
Store Fraud Analysis Result

## Risk Levels

LOW

MEDIUM

HIGH

## Outcome

Claim receives fraud classification.

---

# Flow 9 - Auto Assignment

## Objective

Assign a claim to an investigator.

## Flow

Claim Ready For Assignment
↓
Load Assignment Policy
↓
Determine Region
↓
Determine Branch
↓
Find Eligible Investigators
↓
Apply Assignment Strategy

Round Robin
OR
Least Loaded

↓
Create Assignment
↓
Notify Investigator
↓
Status = AWAITING_ACCEPTANCE

## Outcome

Investigator receives assignment.

---

# Flow 10 - Assignment Acceptance

## Objective

Allow investigator to accept assignment.

## Flow

Investigator Receives Assignment
↓
Review Assignment
↓
Accept Assignment
↓
Status = UNDER_INVESTIGATION

## Outcome

Investigation begins.

---

# Flow 11 - Assignment Rejection

## Objective

Allow investigator to reject assignment.

## Flow

Investigator Receives Assignment
↓
Reject Assignment
↓
Provide Reason
↓
Assignment History Updated
↓
Assignment Engine Triggered
↓
Assign New Investigator

## Outcome

Claim reassigned automatically.

---

# Flow 12 - Investigation Workflow

## Objective

Perform claim investigation.

## Flow

Investigator
↓
Review Claim
↓
Review Documents
↓
Review OCR Results
↓
Review Image Analysis
↓
Review Fraud Score
↓
Add Investigation Notes
↓
Continue Investigation

## Outcome

Evidence gathering completed.

---

# Flow 13 - Additional Information Request

## Objective

Request missing information from customer.

## Flow

Investigator
↓
Request Additional Information
↓
Specify Required Documents
↓
Status = WAITING_FOR_CUSTOMER
↓
Customer Notified

Customer
↓
Upload New Documents
↓
New Document Version Created
↓
OCR Re-Executed
↓
Fraud Analysis Re-Executed
↓
Status = UNDER_INVESTIGATION

## Outcome

Investigation continues with updated information.

---

# Flow 14 - Claim Approval

## Objective

Approve claim.

## Flow

Investigator
↓
Approve Claim
↓
Enter Approval Notes
↓
Approved Amount Recorded
↓
Status = APPROVED
↓
Claim Closed

## Outcome

Claim successfully approved.

---

# Flow 15 - Claim Rejection

## Objective

Reject claim.

## Flow

Investigator
↓
Reject Claim
↓
Enter Rejection Reason
↓
Status = REJECTED
↓
Claim Closed

## Outcome

Claim rejected with full audit trail.

---

# Flow 16 - Claim Reopening

## Objective

Reopen previously closed claim.

## Flow

Customer
OR
Employee
↓
Submit Reopen Request
↓
Manager Review
↓
Approve Reopen
↓
Status = REOPENED
↓
Reassignment Triggered
↓
Investigation Continues

## Outcome

Claim returns to active investigation.

---

# Flow 17 - Manual Reassignment

## Objective

Allow managers to change investigators.

## Flow

Investigation Manager
↓
Review Assignment
↓
Select New Investigator
↓
Create New Assignment
↓
Close Previous Assignment
↓
Notify New Investigator

## Outcome

Claim ownership changes.

---

# Flow 18 - Policy Management

## Objective

Configure claim processing rules.

## Flow

Company Admin
↓
Configure Claim Type Policy
↓
Configure Required Documents
↓
Configure Assignment Rules
↓
Configure Fraud Rules
↓
Configure SLA Rules
↓
Publish Policy Version

## Outcome

New claims use updated policies.

---

# Flow 19 - Notifications

## Objective

Keep users informed.

## Trigger Events

* Claim Submitted
* Assignment Created
* Assignment Reassigned
* Information Requested
* Claim Approved
* Claim Rejected
* Claim Reopened

## Channels

* In-App Notifications
* Email Notifications

---

# Flow 20 - Analytics Generation

## Objective

Generate operational insights.

## Flow

Claims
Assignments
Investigations
Fraud Results
User Activities
↓
Analytics Aggregation
↓
Dashboard Metrics

## Dashboards

Claim Dashboard

Fraud Dashboard

Investigator Dashboard

Operational Dashboard

---

# Flow 21 - Audit Trail Generation

## Objective

Maintain compliance and traceability.

## Events Logged

* Claim Creation
* Status Changes
* Assignments
* Reassignments
* Document Uploads
* Investigation Actions
* Policy Changes
* User Management Actions

## Outcome

Complete audit history available.

---

# Flow 22 - Observability & Monitoring

## Objective

Monitor platform health.

## Metrics

API Latency

Request Rate

Error Rate

Database Pool Health

OCR Processing Time

Fraud Analysis Time

Assignment Time

## Monitoring Stack

Micrometer
↓
Prometheus
↓
Grafana

## Outcome

Platform performance is continuously monitored.

---

# End-To-End ClaimLens Workflow

Customer
↓
Create Claim
↓
Upload Documents
↓
Submit Claim
↓
OCR Processing
↓
Image Analysis
↓
Fraud Analysis
↓
Assignment Engine
↓
Investigator Review
↓
Additional Information Required?

YES
↓
Customer Uploads Additional Documents
↓
Re-Analysis

NO
↓
Approve OR Reject
↓
Close Claim
↓
Analytics Updated
↓
Audit Logs Updated
↓
Observability Metrics Updated

---

# V1 Success Workflow

Customer
↓
Submit Motor Claim
↓
Upload Required Documents
↓
OCR Executes
↓
Image Analysis Executes
↓
Fraud Score Generated
↓
Claim Assigned
↓
Investigator Reviews
↓
Claim Approved / Rejected
↓
Claim Closed

This workflow represents the complete operational scope of ClaimLens Version 1.
