# 14.2 Database Design Part 14 – Insurance Product Domain

Status: Approved

Version: 1.0

Owner: RiskLens Technologies

---

# Purpose

This domain manages:

* Insurance Products
* Product Versioning
* Product Documents
* Policy Traceability

This domain serves as the foundation for:

* Product Management
* Policy Version Control
* Historical Claim Consistency
* Coverage Validation
* Future Policy Intelligence Integration

---

# Why This Domain Exists

A Claim Type alone cannot represent a real insurance offering.

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

but each product may contain:

```text
Different Coverage

Different Exclusions

Different Claim Conditions

Different Policy Documents

Different Fraud Detection Rules
```

Therefore Insurance Product must be modeled as a first-class entity.

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

# Table: insurance_product

Purpose:

Represents an insurance product offered by an insurance company.

Schema:

public

---

Columns

| Column        | Type         | Constraints   |
| ------------- | ------------ | ------------- |
| id            | BIGSERIAL    | PRIMARY KEY   |
| tenant_id     | BIGINT       | NOT NULL      |
| claim_type_id | BIGINT       | NOT NULL      |
| product_code  | VARCHAR(100) | NOT NULL      |
| product_name  | VARCHAR(255) | NOT NULL      |
| description   | TEXT         |               |
| status        | VARCHAR(50)  | NOT NULL      |
| created_at    | TIMESTAMP    | NOT NULL      |
| created_by    | BIGINT       |               |
| updated_at    | TIMESTAMP    |               |
| updated_by    | BIGINT       |               |
| is_deleted    | BOOLEAN      | DEFAULT FALSE |
| deleted_at    | TIMESTAMP    |               |
| deleted_by    | BIGINT       |               |

---

Foreign Keys

```sql
tenant_id
    REFERENCES insurance_company(id)

claim_type_id
    REFERENCES claim_type(id)
```

---

Examples

```text
PRIVATE_CAR_PREMIUM

PRIVATE_CAR_BASIC

COMMERCIAL_VEHICLE_GOLD

COMMERCIAL_VEHICLE_STANDARD
```

---

Status Values

```text
DRAFT

ACTIVE

INACTIVE

RETIRED
```

---

Unique Constraint

```sql
CREATE UNIQUE INDEX uq_insurance_product_code
ON insurance_product (
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

CREATE INDEX idx_product_status
ON insurance_product(status);
```

---

# Table: insurance_product_version

Purpose:

Represents a version of an insurance product.

Insurance policies evolve over time and claims must remain tied to the exact version active at claim creation.

Schema:

public

---

Columns

| Column               | Type         | Constraints   |
| -------------------- | ------------ | ------------- |
| id                   | BIGSERIAL    | PRIMARY KEY   |
| tenant_id            | BIGINT       | NOT NULL      |
| insurance_product_id | BIGINT       | NOT NULL      |
| version_number       | INTEGER      | NOT NULL      |
| version_name         | VARCHAR(255) | NOT NULL      |
| effective_from       | DATE         | NOT NULL      |
| effective_to         | DATE         |               |
| status               | VARCHAR(50)  | NOT NULL      |
| created_at           | TIMESTAMP    | NOT NULL      |
| created_by           | BIGINT       |               |
| updated_at           | TIMESTAMP    |               |
| updated_by           | BIGINT       |               |
| is_deleted           | BOOLEAN      | DEFAULT FALSE |
| deleted_at           | TIMESTAMP    |               |
| deleted_by           | BIGINT       |               |

---

Foreign Keys

```sql
tenant_id
    REFERENCES insurance_company(id)

insurance_product_id
    REFERENCES insurance_product(id)
```

---

Examples

```text
Private Car Premium V1

Private Car Premium V2

Private Car Premium V3
```

---

Status Values

```text
DRAFT

ACTIVE

EXPIRED

RETIRED
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

Business Rules

Only one ACTIVE version per product.

Example:

```text
Private Car Premium

V1 = EXPIRED

V2 = ACTIVE

V3 = DRAFT
```

Valid.

---

Indexes

```sql
CREATE INDEX idx_product_version_product
ON insurance_product_version(
    insurance_product_id
);

CREATE INDEX idx_product_version_status
ON insurance_product_version(status);

CREATE INDEX idx_product_version_effective
ON insurance_product_version(
    effective_from,
    effective_to
);
```

---

# Table: product_document

Purpose:

Associates official documents with a specific product version.

Schema:

public

---

Columns

| Column             | Type         | Constraints   |
| ------------------ | ------------ | ------------- |
| id                 | BIGSERIAL    | PRIMARY KEY   |
| tenant_id          | BIGINT       | NOT NULL      |
| product_version_id | BIGINT       | NOT NULL      |
| document_id        | BIGINT       | NOT NULL      |
| document_type      | VARCHAR(100) | NOT NULL      |
| created_at         | TIMESTAMP    | NOT NULL      |
| created_by         | BIGINT       |               |
| is_deleted         | BOOLEAN      | DEFAULT FALSE |
| deleted_at         | TIMESTAMP    |               |
| deleted_by         | BIGINT       |               |

---

Foreign Keys

```sql
tenant_id
    REFERENCES insurance_company(id)

product_version_id
    REFERENCES insurance_product_version(id)

document_id
    REFERENCES document(id)
```

---

Document Types

```text
POLICY_WORDING

COVERAGE_GUIDE

CLAIM_GUIDELINE

FRAUD_MANUAL

UNDERWRITING_GUIDE

ENDORSEMENT

RIDER_DOCUMENT
```

---

Indexes

```sql
CREATE INDEX idx_product_document_version
ON product_document(product_version_id);

CREATE INDEX idx_product_document_type
ON product_document(document_type);
```

---

# Claim Table Changes

Purpose:

Every claim must remain tied to the exact product version active at claim creation.

---

Add Columns

| Column                       | Type   |
| ---------------------------- | ------ |
| insurance_product_id         | BIGINT |
| insurance_product_version_id | BIGINT |

---

Foreign Keys

```sql
insurance_product_id
    REFERENCES insurance_product(id)

insurance_product_version_id
    REFERENCES insurance_product_version(id)
```

---

Indexes

```sql
CREATE INDEX idx_claim_product
ON claim(insurance_product_id);

CREATE INDEX idx_claim_product_version
ON claim(insurance_product_version_id);
```

---

# Critical Business Rule

When claim is created:

```text
Selected Product
        ↓
Current Active Version
        ↓
Store Both IDs
```

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

becomes active later,

Claim #1001 remains associated with:

```text
V2
```

forever.

---

# Product Lifecycle

```text
Create Product
        ↓
Create Version
        ↓
Upload Product Documents
        ↓
Activate Version
        ↓
Claims Created
        ↓
Version Retired
```

---

# Data Retention Strategy

Insurance Product:

```text
Never Hard Delete
```

Insurance Product Version:

```text
Never Hard Delete
```

Product Documents:

```text
Never Hard Delete
```

Reason:

```text
Regulatory Traceability

Historical Claim Validation

Audit Requirements
```

---

# Future Integration

This domain becomes the foundation for:

```text
Policy Intelligence

Coverage Validation

Policy Copilot

Investigation Copilot

Scenario-Based Policy Questions

Coverage Recommendation Engine
```

---

# Domain Summary

Tables

```text
insurance_product

insurance_product_version

product_document
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

Historical Traceability

```text
Supported
```

Enterprise Insurance Modeling

```text
YES
```
