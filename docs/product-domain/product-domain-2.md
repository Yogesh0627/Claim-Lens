> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; where it diverges from the shipped code the authoritative sources win: [`../domain-model.md`](../domain-model.md), [`../architecture.md`](../architecture.md), [`../audit-report.md`](../audit-report.md), and the running API. Deltas flagged inline as **As-built** notes.

**As-built (2026-07-29):** This file is a near-duplicate of [`insurance-product-domain.md`](./insurance-product-domain.md) (14.1); the two cover the same domain with slightly different emphasis. The one substantive addition here — a `ProductKnowledgeBase` entity — was **not built**: RAG knowledge shipped in the separate `coverage` module as `PolicyChunk` (+ `CoverageAnswer`/`CoverageCitation`), populated from product-version documents. See [`../ai-layer/policy-intelligence-domain.md`](../ai-layer/policy-intelligence-domain.md).

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

**As-built (2026-07-29):** Not built under this name or in this module. Searchable knowledge is the `coverage` module's `PolicyChunk` (with pgvector or in-Java cosine retrieval), keyed to a product version — see the ai-layer docs.

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

**As-built (2026-07-29):** The `product` module ships `InsuranceProduct`, `InsuranceProductVersion`, and `ProductDocument` only. `ProductKnowledgeBase` maps to the `coverage` module's `PolicyChunk`/`CoverageAnswer`/`CoverageCitation`.

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
