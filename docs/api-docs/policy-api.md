> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; where it diverges from the shipped code the authoritative sources win: [`../domain-model.md`](../domain-model.md), [`../architecture.md`](../architecture.md), [`../audit-report.md`](../audit-report.md), and the running API. Concrete divergences are flagged inline as **As-built** notes.

# 07.5 Policy Engine API

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | Niyo Technologies |
| Version         | V1                    |
| Document Type   | API Design            |
| Document Number | 07.5                  |
| Document Name   | Policy Engine API     |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

The Policy Engine is the configuration backbone of ClaimLens.

Rather than hardcoding business rules into application code, operational behavior is controlled through configurable policies stored in the database.

**As-built (2026-07-29): critical naming correction.** This document's "Policy Engine" describes *configuration rules* under `/api/v1/policies/...`, but in the shipped system **`/policies` is a different resource entirely** — it is the **insurance-contract policy** (the customer's cover), module `policy`, guarded by `POLICY_{READ,WRITE}` (`POST /policies`, `GET /policies/{id}`, `GET /policies?page&size`, `GET /policies/options`, `POST /policies/{id}/cancel`). See the Insurance Policy API doc for that.
>
> The **fraud-configuration** portion of this doc (fraud policies + fraud rules, sections 8–9) shipped as the **fraud ruleset**, in module `ruleset`, at **`/api/v1/rulesets/fraud`** — **not** under `/policies` — guarded by `RULESET_{READ,WRITE}`:
> - `POST /rulesets/fraud` (`RULESET_WRITE`) — create a ruleset (rules embedded, no separate `fraud_rule` endpoints).
> - `POST /rulesets/fraud/{id}/activate` (`RULESET_WRITE`).
> - `GET /rulesets/fraud/{id}` (`RULESET_READ`) · `GET /rulesets/fraud` (`RULESET_READ`).
> - `GET /rulesets/fraud/rule-catalog` (`RULESET_READ`) — the catalog of available rule codes.
>
> The remaining config surfaces in this doc — claim-types, document-types, required-document policies, assignment policies, SLA policies, analysis strategies, and the generic activate/deactivate endpoints — were **not built as runtime APIs**; those behaviors are seeded/handled internally. Sections 4–7 and 10–12 below therefore have no corresponding endpoints. The fraud-evaluation *dataset* is exposed read-only at `GET /fraud/evaluation/dataset` (`FRAUD_READ`).

The Policy Engine controls:

* Supported Claim Types
* Required Documents
* Assignment Rules
* Fraud Detection Rules
* SLA Rules
* Analysis Strategies
* Processing Configuration

This enables insurance companies to modify business behavior without requiring software deployments.

---

# 2. Domain Entities

Managed Entities:

```text
claim_type
claim_type_policy
document_type
required_document_policy
assignment_policy
fraud_policy
fraud_rule
sla_policy
analysis_strategy
```

---

# 3. Permission Matrix

| Permission        | Description         |
| ----------------- | ------------------- |
| POLICY_VIEW       | View policies       |
| POLICY_CREATE     | Create policies     |
| POLICY_UPDATE     | Update policies     |
| POLICY_DELETE     | Delete policies     |
| FRAUD_RULE_MANAGE | Manage fraud rules  |
| SLA_POLICY_MANAGE | Manage SLA policies |

**As-built (2026-07-29):** these permission codes do not exist as written. Fraud-config authorization uses **`RULESET_READ` / `RULESET_WRITE`**. The `POLICY_READ` / `POLICY_WRITE` codes that *do* exist govern the unrelated **insurance-contract policy** resource, not this config engine. There are no `FRAUD_RULE_MANAGE` or `SLA_POLICY_MANAGE` permissions.

---

# 4. Claim Type APIs

**As-built (2026-07-29):** not built as runtime CRUD endpoints. Claim types (`ClaimType`) are reference data; V1 supports `MOTOR` only. No `/policies/claim-types` endpoints exist.

Claim Types define categories of claims supported by the platform.

V1 supports:

```text
MOTOR
```

Future versions may support:

```text
HEALTH
PROPERTY
LIFE
TRAVEL
```

---

## Create Claim Type

### Endpoint

```http
POST /api/v1/policies/claim-types
```

### Request Body

```json
{
  "claimTypeCode": "MOTOR",
  "claimTypeName": "Motor Insurance",
  "description": "Motor vehicle insurance claims"
}
```

### Success Response

```json
{
  "claimTypeId": 1,
  "status": "ACTIVE"
}
```

### Audit Event

```text
CLAIM_TYPE_CREATED
```

---

## Get Claim Type

### Endpoint

```http
GET /api/v1/policies/claim-types/{claimTypeId}
```

---

## Update Claim Type

### Endpoint

```http
PUT /api/v1/policies/claim-types/{claimTypeId}
```

### Audit Event

```text
CLAIM_TYPE_UPDATED
```

---

## List Claim Types

### Endpoint

```http
GET /api/v1/policies/claim-types
```

---

# 5. Document Type APIs

**As-built (2026-07-29):** not built as runtime endpoints — document typing is handled internally, not via `/policies/document-types`.

Document Types represent uploadable document categories.

Examples:

```text
DRIVING_LICENSE
VEHICLE_RC
INSURANCE_POLICY
FIR
REPAIR_ESTIMATE
VEHICLE_IMAGES
```

---

## Create Document Type

### Endpoint

```http
POST /api/v1/policies/document-types
```

### Request Body

```json
{
  "documentTypeCode": "DRIVING_LICENSE",
  "documentTypeName": "Driving License",
  "allowedExtensions": [
    "pdf",
    "jpg",
    "jpeg",
    "png"
  ],
  "maxFileSizeMb": 20
}
```

### Success Response

```json
{
  "documentTypeId": 11
}
```

### Audit Event

```text
DOCUMENT_TYPE_CREATED
```

---

## List Document Types

### Endpoint

```http
GET /api/v1/policies/document-types
```

### Query Parameters

```text
claimTypeId
status
```

---

# 6. Required Document Policy APIs

**As-built (2026-07-29):** not built as runtime endpoints — no `/policies/required-document-policies`.

Controls which documents are mandatory for a claim type.

Example:

```text
MOTOR CLAIM
 ├── DRIVING_LICENSE
 ├── VEHICLE_RC
 ├── INSURANCE_POLICY
 └── VEHICLE_IMAGES
```

---

## Create Required Document Policy

### Endpoint

```http
POST /api/v1/policies/required-document-policies
```

### Request Body

```json
{
  "claimTypeId": 1,
  "documentTypeId": 11,
  "isMandatory": true
}
```

### Success Response

```json
{
  "policyId": 101
}
```

### Audit Event

```text
REQUIRED_DOCUMENT_POLICY_CREATED
```

---

## Get Required Documents For Claim Type

### Endpoint

```http
GET /api/v1/policies/claim-types/{claimTypeId}/required-documents
```

### Success Response

```json
[
  {
    "documentTypeId": 11,
    "documentTypeCode": "DRIVING_LICENSE",
    "isMandatory": true
  }
]
```

---

# 7. Assignment Policy APIs

**As-built (2026-07-29):** not built as `/policies/assignment-policies` config endpoints. Assignment is done through the claim assignment APIs (`POST /claims/{id}/assign`, `/auto-assign`, `/reassign`, all `CLAIM_ASSIGN`) rather than a configurable assignment-policy resource.

Assignment policies control investigator allocation.

Supported Assignment Strategies:

```text
ROUND_ROBIN
LEAST_WORKLOAD
REGION_BASED
BRANCH_BASED
MANUAL
```

---

## Create Assignment Policy

### Endpoint

```http
POST /api/v1/policies/assignment-policies
```

### Request Body

```json
{
  "claimTypeId": 1,
  "assignmentStrategy": "LEAST_WORKLOAD",
  "maxAssignmentsPerInvestigator": 50
}
```

### Success Response

```json
{
  "assignmentPolicyId": 201
}
```

### Audit Event

```text
ASSIGNMENT_POLICY_CREATED
```

---

## Get Assignment Policy

### Endpoint

```http
GET /api/v1/policies/assignment-policies/{policyId}
```

---

## Update Assignment Policy

### Endpoint

```http
PUT /api/v1/policies/assignment-policies/{policyId}
```

### Audit Event

```text
ASSIGNMENT_POLICY_UPDATED
```

---

# 8. Fraud Policy APIs

**As-built (2026-07-29):** shipped as the **fraud ruleset**, not a `fraud_policy` under `/policies`. Create/read/list/activate a ruleset via **`/api/v1/rulesets/fraud`** (`RULESET_WRITE` for create + `POST /rulesets/fraud/{id}/activate`; `RULESET_READ` for `GET /rulesets/fraud` and `GET /rulesets/fraud/{id}`). Thresholds map to the `LOW/MEDIUM/HIGH` bands (no `CRITICAL`).

Fraud policies determine fraud scoring behavior.

Fraud policy acts as a container for fraud rules.

---

## Create Fraud Policy

### Endpoint

```http
POST /api/v1/policies/fraud-policies
```

### Request Body

```json
{
  "policyName": "Motor Fraud Policy",
  "claimTypeId": 1,
  "fraudThreshold": 70,
  "highRiskThreshold": 90
}
```

### Success Response

```json
{
  "fraudPolicyId": 301
}
```

### Audit Event

```text
FRAUD_POLICY_CREATED
```

---

## Get Fraud Policy

### Endpoint

```http
GET /api/v1/policies/fraud-policies/{policyId}
```

---

## Update Fraud Policy

### Endpoint

```http
PUT /api/v1/policies/fraud-policies/{policyId}
```

### Audit Event

```text
FRAUD_POLICY_UPDATED
```

---

# 9. Fraud Rule APIs

**As-built (2026-07-29):** there are **no standalone `/policies/fraud-rules` endpoints**. Rules (`FraudRule`) are embedded in a `FraudRuleset` and created/updated as part of the ruleset payload at `/rulesets/fraud` (`RULESET_WRITE`). The catalog of available rule codes is read via **`GET /rulesets/fraud/rule-catalog`** (`RULESET_READ`). There are no separate create/update/enable/disable rule endpoints.

Fraud Rules are executed by the Fraud Engine.

Examples:

```text
DUPLICATE_DOCUMENT
OCR_CONFIDENCE_LOW
IMAGE_TAMPERING
MULTIPLE_CLAIMS_SAME_VEHICLE
MISSING_REQUIRED_DOCUMENT
```

---

## Create Fraud Rule

### Endpoint

```http
POST /api/v1/policies/fraud-rules
```

### Request Body

```json
{
  "fraudPolicyId": 301,
  "ruleCode": "OCR_CONFIDENCE_LOW",
  "ruleName": "Low OCR Confidence",
  "riskWeight": 15,
  "enabled": true
}
```

### Success Response

```json
{
  "fraudRuleId": 401
}
```

### Audit Event

```text
FRAUD_RULE_CREATED
```

---

## Update Fraud Rule

### Endpoint

```http
PUT /api/v1/policies/fraud-rules/{ruleId}
```

### Audit Event

```text
FRAUD_RULE_UPDATED
```

---

## Enable Fraud Rule

### Endpoint

```http
PATCH /api/v1/policies/fraud-rules/{ruleId}/enable
```

### Success Response

```json
{
  "enabled": true
}
```

### Audit Event

```text
FRAUD_RULE_ENABLED
```

---

## Disable Fraud Rule

### Endpoint

```http
PATCH /api/v1/policies/fraud-rules/{ruleId}/disable
```

### Success Response

```json
{
  "enabled": false
}
```

### Audit Event

```text
FRAUD_RULE_DISABLED
```

---

## List Fraud Rules

### Endpoint

```http
GET /api/v1/policies/fraud-rules
```

### Filters

```text
fraudPolicyId
enabled
claimTypeId
```

---

# 10. SLA Policy APIs

**As-built (2026-07-29):** not built — no `/policies/sla-policies` endpoints.

SLA policies define investigation deadlines.

Example:

```text
LOW RISK CLAIM      = 3 DAYS
MEDIUM RISK CLAIM   = 5 DAYS
HIGH RISK CLAIM     = 7 DAYS
```

---

## Create SLA Policy

### Endpoint

```http
POST /api/v1/policies/sla-policies
```

### Request Body

```json
{
  "claimTypeId": 1,
  "riskLevel": "HIGH",
  "targetHours": 168
}
```

### Success Response

```json
{
  "slaPolicyId": 501
}
```

### Audit Event

```text
SLA_POLICY_CREATED
```

---

## Update SLA Policy

### Endpoint

```http
PUT /api/v1/policies/sla-policies/{policyId}
```

### Audit Event

```text
SLA_POLICY_UPDATED
```

---

## List SLA Policies

### Endpoint

```http
GET /api/v1/policies/sla-policies
```

---

# 11. Analysis Strategy APIs

**As-built (2026-07-29):** not built as config endpoints. Which checks run (per-document OCR + per-image analysis) is fixed by the processing pipeline, not a `/policies/analysis-strategies` resource.

Analysis Strategies determine which automated checks execute during claim processing.

Available Analysis Types:

```text
OCR_EXTRACTION
IMAGE_METADATA_ANALYSIS
IMAGE_TAMPERING_ANALYSIS
DUPLICATE_IMAGE_ANALYSIS
DOCUMENT_COMPLETENESS_CHECK
```

---

## Create Analysis Strategy

### Endpoint

```http
POST /api/v1/policies/analysis-strategies
```

### Request Body

```json
{
  "claimTypeId": 1,
  "strategyCode": "IMAGE_TAMPERING_ANALYSIS",
  "enabled": true,
  "executionOrder": 3
}
```

### Success Response

```json
{
  "analysisStrategyId": 601
}
```

### Audit Event

```text
ANALYSIS_STRATEGY_CREATED
```

---

## Update Analysis Strategy

### Endpoint

```http
PUT /api/v1/policies/analysis-strategies/{strategyId}
```

### Audit Event

```text
ANALYSIS_STRATEGY_UPDATED
```

---

## List Analysis Strategies

### Endpoint

```http
GET /api/v1/policies/analysis-strategies
```

### Filters

```text
claimTypeId
enabled
```

---

# 12. Policy Activation APIs

**As-built (2026-07-29):** the generic `/policies/{policyType}/{policyId}/activate|deactivate` endpoints were not built. The only activation that ships is ruleset activation: **`POST /rulesets/fraud/{id}/activate`** (`RULESET_WRITE`), which makes one fraud ruleset the active one.

Policies support versioning and activation.

Only one active version may exist per policy type.

---

## Activate Policy

### Endpoint

```http
PATCH /api/v1/policies/{policyType}/{policyId}/activate
```

### Success Response

```json
{
  "status": "ACTIVE"
}
```

### Audit Event

```text
POLICY_ACTIVATED
```

---

## Deactivate Policy

### Endpoint

```http
PATCH /api/v1/policies/{policyType}/{policyId}/deactivate
```

### Success Response

```json
{
  "status": "INACTIVE"
}
```

### Audit Event

```text
POLICY_DEACTIVATED
```

---

# 13. Business Validation Rules

## Claim Type

* Claim type code must be unique.
* Claim type name must be unique within tenant.

## Document Type

* Document type code unique within tenant.
* Max file size cannot exceed platform limit.

## Assignment Policy

* Only one active assignment policy per claim type.

## Fraud Policy

* Fraud threshold must be less than high risk threshold.
* Only one active fraud policy per claim type.

## Fraud Rule

* Rule code unique within fraud policy.
* Risk weight between 1 and 100.

## SLA Policy

* Risk level unique per claim type.

## Analysis Strategy

* Execution order must be unique within claim type.

---

# 14. Error Codes

| Error Code                  | Description                  |
| --------------------------- | ---------------------------- |
| CLAIM_TYPE_NOT_FOUND        | Claim type missing           |
| DOCUMENT_TYPE_NOT_FOUND     | Document type missing        |
| FRAUD_POLICY_NOT_FOUND      | Fraud policy missing         |
| FRAUD_RULE_NOT_FOUND        | Fraud rule missing           |
| SLA_POLICY_NOT_FOUND        | SLA policy missing           |
| ANALYSIS_STRATEGY_NOT_FOUND | Analysis strategy missing    |
| DUPLICATE_POLICY_CODE       | Duplicate policy code        |
| INVALID_RISK_WEIGHT         | Invalid risk weight          |
| INVALID_THRESHOLD           | Invalid fraud threshold      |
| ACTIVE_POLICY_EXISTS        | Active policy already exists |
| POLICY_IN_USE               | Policy referenced by claims  |

---

# 15. Audit Events

```text
CLAIM_TYPE_CREATED
CLAIM_TYPE_UPDATED

DOCUMENT_TYPE_CREATED
DOCUMENT_TYPE_UPDATED

REQUIRED_DOCUMENT_POLICY_CREATED
REQUIRED_DOCUMENT_POLICY_UPDATED

ASSIGNMENT_POLICY_CREATED
ASSIGNMENT_POLICY_UPDATED

FRAUD_POLICY_CREATED
FRAUD_POLICY_UPDATED

FRAUD_RULE_CREATED
FRAUD_RULE_UPDATED
FRAUD_RULE_ENABLED
FRAUD_RULE_DISABLED

SLA_POLICY_CREATED
SLA_POLICY_UPDATED

ANALYSIS_STRATEGY_CREATED
ANALYSIS_STRATEGY_UPDATED

POLICY_ACTIVATED
POLICY_DEACTIVATED
```

---

# 16. Performance Requirements

| API                      | Target   |
| ------------------------ | -------- |
| Policy Creation          | < 500 ms |
| Policy Update            | < 300 ms |
| Policy Retrieval         | < 200 ms |
| Fraud Rule Listing       | < 300 ms |
| Assignment Policy Lookup | < 100 ms |
| SLA Policy Lookup        | < 100 ms |

---

# 17. Security Requirements

All APIs require:

```text
JWT Authentication
Tenant Validation
Permission Validation
Audit Logging
```

Additional Controls:

* Policy changes restricted to administrative roles.
* Cross-tenant policy access prohibited.
* Fraud rules require elevated permissions.
* All policy modifications must be auditable.

---

# Approval

This document defines the Policy Engine API contract for ClaimLens and serves as the implementation reference for configurable business rules, fraud detection policies, assignment strategies, SLA management, document requirements, and automated processing strategies.
