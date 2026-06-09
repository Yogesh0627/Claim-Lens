# Database Design Part 14 - Insurance Product & Policy Domain

Status: Draft

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

This domain manages:

* Insurance Products
* Product Versioning
* Policy Documents
* Coverage Knowledge Base
* Policy RAG Integration

This domain is the foundation for:

* Product Management
* Policy Management
* Coverage Validation
* Policy Copilot
* Investigation Copilot
* Policy Version Traceability

---

# Why This Domain Exists

A Claim does not belong only to a Claim Type.

Example:

Claim Type

```text
MOTOR
```

Products

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

but each product has:

```text
Different Coverage

Different Exclusions

Different Conditions

Different Policy Wordings
```

Therefore Product must be a first-class domain.

---

# Product Hierarchy

```text
Insurance Company
        ↓
Insurance Product
        ↓
Product Version
        ↓
Policy Documents
        ↓
RAG Knowledge Base
```

---

# Table: insurance_product

Purpose:

Represents an insurance product offered by a company.

Schema:

public

---

Columns

| Column        | Type         | Constraints |
| ------------- | ------------ | ----------- |
| id            | BIGSERIAL    | PRIMARY KEY |
| tenant_id     | BIGINT       | NOT NULL    |
| claim_type_id | BIGINT       | NOT NULL    |
| product_code  | VARCHAR(100) | NOT NULL    |
| product_name  | VARCHAR(255) | NOT NULL    |
| description   | TEXT         |             |
| status        | VARCHAR(50)  | NOT NULL    |
| created_at    | TIMESTAMP    | NOT NULL    |
| created_by    | BIGINT       |             |
| updated_at    | TIMESTAMP    |             |
| updated_by    | BIGINT       |             |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)

claim_type_id
    → claim_type(id)
```

---

Examples

```text
PRIVATE_CAR_PREMIUM

PRIVATE_CAR_BASIC

COMMERCIAL_VEHICLE_GOLD
```

---

Unique Constraint

```sql
CREATE UNIQUE INDEX uq_product_code
ON insurance_product(
    tenant_id,
    product_code
);
```

---

Indexes

```sql
CREATE INDEX idx_product_tenant
ON insurance_product(tenant_id);

CREATE INDEX idx_product_claim_type
ON insurance_product(claim_type_id);
```

---

# Table: insurance_product_version

Purpose:

Represents a specific version of a product.

Policy rules can change over time.

Claims must always reference the exact version active at claim creation.

Schema:

public

---

Columns

| Column               | Type         |
| -------------------- | ------------ |
| id                   | BIGSERIAL    |
| insurance_product_id | BIGINT       |
| version_number       | INTEGER      |
| version_name         | VARCHAR(255) |
| effective_from       | DATE         |
| effective_to         | DATE         |
| status               | VARCHAR(50)  |
| created_at           | TIMESTAMP    |

---

Foreign Keys

```sql
insurance_product_id
    → insurance_product(id)
```

---

Examples

```text
Version 1
Effective: 2026-01-01

Version 2
Effective: 2026-07-01

Version 3
Effective: 2027-01-01
```

---

Unique Constraint

```sql
CREATE UNIQUE INDEX uq_product_version
ON insurance_product_version(
    insurance_product_id,
    version_number
);
```

---

# Table: product_document

Purpose:

Stores documents associated with a product version.

Examples:

```text
Policy Wording

Claim Guidelines

Coverage Guide

Fraud SOP
```

Schema:

public

---

Columns

| Column             | Type         |
| ------------------ | ------------ |
| id                 | BIGSERIAL    |
| product_version_id | BIGINT       |
| document_id        | BIGINT       |
| document_type      | VARCHAR(100) |
| created_at         | TIMESTAMP    |

---

Foreign Keys

```sql
product_version_id
    → insurance_product_version(id)

document_id
    → document(id)
```

---

Document Types

```text
POLICY_WORDING

CLAIM_GUIDELINE

COVERAGE_GUIDE

FRAUD_MANUAL

UNDERWRITING_GUIDE
```

---

# Table: product_knowledge_base

Purpose:

Stores references used by RAG.

Schema:

knowledge

---

Columns

| Column             | Type         |
| ------------------ | ------------ |
| id                 | BIGSERIAL    |
| product_version_id | BIGINT       |
| chunk_id           | VARCHAR(255) |
| source_document_id | BIGINT       |
| created_at         | TIMESTAMP    |

---

Purpose

Allows:

```text
Policy Copilot

Coverage Assistant

Scenario Based Questions

Investigation Copilot
```

to retrieve product-specific knowledge.

---

# Claim Domain Changes

The claim table must be updated.

Add Columns

| Column                       |
| ---------------------------- |
| insurance_product_id         |
| insurance_product_version_id |

Foreign Keys

```sql
insurance_product_id
    → insurance_product(id)

insurance_product_version_id
    → insurance_product_version(id)
```

---

# Critical Business Rule

When a claim is created:

```text
Claim
     ↓
Product
     ↓
Current Active Product Version
```

Store BOTH IDs.

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

is uploaded later,

Claim #1001 always remains tied to:

```text
V2
```

---

# Policy Upload Flow

Company Admin

↓

Create Product

↓

Create Product Version

↓

Upload Policy Documents

↓

OCR

↓

Chunking

↓

Embeddings

↓

Vector Database

↓

Knowledge Base Ready

---

# Policy Copilot Flow

User asks:

```text
Is flood damage covered?
```

System:

```text
Claim
    ↓
Product Version
    ↓
Policy Documents
    ↓
Vector Search
    ↓
Gemini
```

Returns grounded answer.

---

# Future AI Capabilities

Supports

```text
Coverage Validation

Policy Assistant

Hypothetical Scenarios

Claim Eligibility Questions

Investigation Copilot

Fraud Copilot
```

---

# Domain Summary

Tables

```text
insurance_product

insurance_product_version

product_document

product_knowledge_base
```

Claim Changes

```text
insurance_product_id

insurance_product_version_id
```

Versioning

```text
Supported
```

Policy RAG

```text
Supported
```

Enterprise Insurance Modeling

```text
YES
```
