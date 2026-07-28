# ClaimLens - Business Flows

Status: Draft

Version: 1.0

Owner: Niyo Technologies

Last Updated: June 2026

---

# Purpose

This document defines the end-to-end business workflows of ClaimLens V1.

The workflows described here represent how users, investigators, managers, and system services interact throughout the claim lifecycle.

**As-built (2026-07-29):** The claim lifecycle is the `ClaimStatus` enum, which defines **11 states**: DRAFT, SUBMITTED, AWAITING_ANALYSIS, AWAITING_ASSIGNMENT, AWAITING_ACCEPTANCE, UNDER_INVESTIGATION, WAITING_FOR_CUSTOMER, APPROVED, REJECTED, CLOSED, REOPENED (`isTerminal()` = APPROVED | REJECTED | CLOSED — REOPENED is intentionally non-terminal). The exercised path is **DRAFT → AWAITING_ANALYSIS → AWAITING_ASSIGNMENT → UNDER_INVESTIGATION → APPROVED | REJECTED**, with **WAITING_FOR_CUSTOMER** as a two-way detour off UNDER_INVESTIGATION. The other constants — **SUBMITTED**, **AWAITING_ACCEPTANCE**, **CLOSED**, **REOPENED** — are defined in the enum and the `claims.status` DB check-constraint, but no implemented endpoint/service currently writes them: submit jumps straight to AWAITING_ANALYSIS, assignment goes straight to UNDER_INVESTIGATION, and there is no accept or close transition. REOPENED and the `reopened_at` column exist as lifecycle/schema support for the reopen flow (Flow 16), which is not yet wired to an API.

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

**As-built (2026-07-29):** The claim number is generated at draft creation (`createDraftInternal`), not at submission. Submit runs the hard/soft checks in `ClaimSubmissionValidator`: HARD failures (reject; no claim is submitted) are incident-date-in-future, an open **DUPLICATE_CLAIM_EXISTS** (same policy + normalized vehicle registration + incident date as a non-terminal claim; settled APPROVED/REJECTED/CLOSED claims do not block), policy not active on the loss date, claimant not the policyholder, and vehicle not covered by the policy. Over-sum-insured is a **SOFT** signal only (returned as a warning, never a reject). There is no required-documents hard gate at submit. On success the status moves straight to **AWAITING_ANALYSIS** (the `SUBMITTED` enum value is skipped) and background processing (OCR + analysis, then fraud) starts.

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

**As-built (2026-07-29):** Fraud runs only behind an **atomic gate** — a conditional UPDATE on the single per-claim processing-state row queues exactly one fraud job once BOTH the OCR and image-analysis stages are COMPLETE. `FraudEngine.evaluate` loads the tenant's ACTIVE ruleset for the claim type (or built-in default weights/thresholds when none is configured), sums the weights of the rules that trigger, maps the total to LOW/MEDIUM/HIGH by the ruleset thresholds, and stores an explainable `FraudScore` (a per-rule explanation string). It then advances the claim to **AWAITING_ASSIGNMENT**. The gate settles on a terminal job outcome (succeeded or retries-exhausted) so it never hangs.

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

**As-built (2026-07-29):** The V1 `AssignmentEngine` strategy is **least-loaded only** (fewest active ASSIGNED assignments among ACTIVE investigators in the tenant); region/branch filters and round-robin are deferred to the assignment ruleset. Assignment is allowed only from **AWAITING_ASSIGNMENT** and moves the claim **directly to UNDER_INVESTIGATION** — there is no AWAITING_ACCEPTANCE step (that enum value is never set). Both a manual `assign` (a specific investigator, guarded so the assignee must hold CLAIM_INVESTIGATE) and `auto-assign` exist; the chosen investigator is notified.

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

**As-built (2026-07-29):** Not built as a distinct step in V1. There is no explicit accept action and no AWAITING_ACCEPTANCE state in the flow — an assignment takes effect immediately, and the claim is already UNDER_INVESTIGATION the moment it is assigned.

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

**As-built (2026-07-29):** Investigator self-rejection is not built. The only reassignment path is the manager-driven `reassign` endpoint (see Flow 17), allowed only while the claim is UNDER_INVESTIGATION: it retires the live assignment(s) as REASSIGNED, creates a new one (a named investigator, or the auto-picked least-loaded investigator when none is given), and notifies the new investigator. `AssignmentStatus` has only ASSIGNED / REASSIGNED / COMPLETED — there is no REJECTED status.

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

**As-built (2026-07-29):** `requestInformation` (CLAIM_INVESTIGATE) is allowed only while the claim is UNDER_INVESTIGATION and moves it to WAITING_FOR_CUSTOMER. The customer answers through the portal (`uploadToMyClaim`), which stores the new document version and — only if the claim was WAITING_FOR_CUSTOMER — re-queues **both OCR and image analysis** over the full, updated document set; fraud re-evaluates through the same atomic gate before the claim returns to UNDER_INVESTIGATION and the assigned investigator is notified.

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

**As-built (2026-07-29):** `decide` (CLAIM_DECIDE) is allowed only from UNDER_INVESTIGATION. Approval sets `approvedAt`, records the fraud label `fraud_confirmed = false`, and moves the claim to **APPROVED**, which is itself terminal — there is no separate CLOSED transition (the CLOSED enum value is unused by V1 flows). The decision does not write a distinct approved-amount field.

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

**As-built (2026-07-29):** Rejection sets `rejectedAt` and records the fraud label `fraud_confirmed` from the request — true only when the investigator confirms fraud, otherwise the rejection is a plain coverage denial. The claim moves to **REJECTED**, which is terminal; there is no separate CLOSED step.

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

**As-built (2026-07-29):** REOPENED is a real, intentionally non-terminal lifecycle state, and the schema carries the `REOPENED` check-constraint value and a `reopened_at` column for it. In the current backend, however, no endpoint or service method transitions a claim into REOPENED — the reopen action (reopen request → manager approval → REOPENED → reassignment) is defined at the lifecycle/schema level but is not yet wired to an API. Note that the info-request loop in Flow 13 (returning a WAITING_FOR_CUSTOMER claim to UNDER_INVESTIGATION) is a separate mechanism, not the REOPENED transition.

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

**As-built (2026-07-29):** The claim events actually emitted are SUBMITTED, ASSIGNED (on both assign and reassign), INFO_REQUESTED, CUSTOMER_RESPONDED, APPROVED, and REJECTED — each delivered both in-app and (best-effort) by email. "Assignment Reassigned" maps to the ASSIGNED event on reassign; "Claim Reopened" is not emitted because the reopen transition is not wired (see Flow 16).

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
