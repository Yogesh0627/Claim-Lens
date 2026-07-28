> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; where it diverges from the shipped code the authoritative sources win: [`../domain-model.md`](../domain-model.md), [`../architecture.md`](../architecture.md), [`../audit-report.md`](../audit-report.md), and the running API. Deltas flagged inline as **As-built** notes.

# 14.1 Insurance Product Domain

Status: Approved

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

The Insurance Product Domain manages:

* Insurance Products
* Product Versioning
* Product Documents
* Policy Lifecycle
* Product Traceability

This domain acts as the bridge between:

```text
Insurance Company
        ↓
Insurance Product
        ↓
Insurance Product Version
        ↓
Claim
        ↓
Investigation
```

and enables:

* Product Management
* Policy Version Traceability
* Coverage Consistency
* Historical Claim Accuracy
* Future Policy Intelligence Integration

---

# Why This Domain Exists

A Claim Type is not sufficient to represent an insurance offering.

Example:

Claim Type:

```text
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

but each product contains:

```text
Different Coverage

Different Exclusions

Different Conditions

Different Policy Documents

Different Fraud Rules

Different Claim Handling Procedures
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

# Product Hierarchy

```text
Insurance Company
        ↓
Insurance Product
        ↓
Insurance Product Version
        ↓
Product Documents
```

---

# Domain Structure

The Insurance Product Domain consists of:

```text
InsuranceProduct

InsuranceProductVersion

ProductDocument
```

**As-built (2026-07-29):** These three entities shipped as described (package `com.niyotechnologies.claimlens.product`). The actual insurance *contract* — `InsurancePolicy` and its `InsuredVehicle` — lives in the separate `policy` module and is what a claim is filed against; a policy references a product version, and that is where version pinning originates (see the Critical Business Rule note below).

---

# InsuranceProduct

Represents an insurance product offered by an insurance company.

Examples:

```text
Private Car Premium

Private Car Basic

Commercial Vehicle Gold

Commercial Vehicle Standard
```

Responsibilities:

```text
Product Identity

Product Lifecycle

Product Ownership

Claim Type Mapping
```

Relationships:

```text
InsuranceCompany
        ↓
InsuranceProduct
```

---

# InsuranceProductVersion

Represents a version of an insurance product.

Purpose:

Insurance policies evolve over time.

Coverage terms may change.

Claims must always remain associated with the exact product version active at claim creation.

Examples:

```text
Private Car Premium V1

Private Car Premium V2

Private Car Premium V3
```

Responsibilities:

```text
Version Control

Effective Date Management

Policy Traceability

Historical Consistency
```

Relationships:

```text
InsuranceProduct
        ↓
InsuranceProductVersion
```

---

# ProductDocument

Represents official documents attached to a product version.

Examples:

```text
Policy Wording

Coverage Guide

Claim Guidelines

Fraud Manual

Underwriting Guide
```

Responsibilities:

```text
Document Association

Policy Reference

Product Knowledge Source
```

Relationships:

```text
InsuranceProductVersion
        ↓
ProductDocument
```

---

# Product Versioning

Example:

Product:

```text
Private Car Premium
```

Version History:

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

must remain associated with:

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

Current:

```text
Claim
    ↓
ClaimType
```

Updated:

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

# Claim Aggregate Changes

The Claim Aggregate must store:

```text
insuranceProductId

insuranceProductVersionId
```

Purpose:

```text
Historical Traceability

Coverage Accuracy

Version Consistency

Auditability
```

---

# Critical Business Rule

At claim creation:

```text
Claim
      ↓
Selected Product
      ↓
Current Active Product Version
```

Both identifiers must be persisted.

**As-built (2026-07-29):** The pinned version comes from the **policy** the claim is filed under (the `InsurancePolicy`'s product version), **not** the product's currently-active version at claim time. This is what guarantees a claim keeps V2 even after V3 is activated — version pinning is anchored on the contract, not resolved live from the product.

Example:

```text
Claim #1001

Product:
Private Car Premium

Version:
V2
```

Even if:

```text
V3
```

is released later,

Claim #1001 always remains linked to:

```text
V2
```

---

# Future Integration

This domain becomes the foundation for:

```text
Policy Intelligence

Coverage Validation

Policy Copilot

Investigation Copilot

Scenario Analysis

Product-Specific Fraud Analysis
```

---

# Future Extensions

Potential V2 entities:

```text
CoverageRule

ExclusionRule

BenefitCatalog

UnderwritingRule

PremiumConfiguration
```

These may be:

```text
Explicitly Modeled
```

or

```text
Derived Through Policy Intelligence
```

depending on future requirements.

---

# Domain Summary

Entities:

```text
InsuranceProduct

InsuranceProductVersion

ProductDocument
```

Supports:

```text
Product Management

Product Versioning

Policy Traceability

Historical Claim Accuracy
```

Enterprise Insurance Modeling:

```text
YES
```
