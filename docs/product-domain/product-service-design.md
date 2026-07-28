> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; where it diverges from the shipped code the authoritative sources win: [`../domain-model.md`](../domain-model.md), [`../architecture.md`](../architecture.md), [`../audit-report.md`](../audit-report.md), and the running API. Deltas flagged inline as **As-built** notes.

# 14.4 Insurance Product Service Design

## Document Information

| Field           | Value                            |
| --------------- | -------------------------------- |
| Project         | ClaimLens                        |
| Company         | Niyo Technologies            |
| Version         | V1                               |
| Document Type   | Service Design                   |
| Document Number | 14.4                             |
| Document Name   | Insurance Product Service Design |
| Status          | Approved                         |
| Last Updated    | June 2026                        |

---

# 1. Overview

The Insurance Product Module manages insurance products, product versions, and product documents.

Responsibilities:

* Product Management
* Product Version Management
* Product Lifecycle Management
* Product Document Association
* Product Validation
* Claim Product Resolution
* Policy Traceability

The Insurance Product Module acts as the authoritative source of insurance product configuration.

---

# 2. Module Dependencies

## Consumed Events

```text
DOCUMENT_UPLOADED

DOCUMENT_VERSION_CREATED
```

---

## Published Events

```text
PRODUCT_CREATED

PRODUCT_UPDATED

PRODUCT_RETIRED

PRODUCT_VERSION_CREATED

PRODUCT_VERSION_ACTIVATED

PRODUCT_VERSION_RETIRED

PRODUCT_DOCUMENT_ATTACHED

PRODUCT_DOCUMENT_REMOVED
```

---

# 3. Package Structure

**As-built (2026-07-29):** The `product` package ships `ProductService` + `ProductServiceImpl` (product **and** version lifecycle folded together — no separate `ProductVersionService`/`ProductLifecycleService`), `ProductDocumentService`, controllers `ProductController` + `ProductDocumentController`, repositories (`InsuranceProductRepository`, `InsuranceProductVersionRepository`, `ProductDocumentRepository`, `ClaimTypeRepository`), entities (`InsuranceProduct`, `InsuranceProductVersion`, `ProductDocument`, `ClaimType`), `enums` (`ProductStatus`, `ProductVersionStatus`), `dto`, and `mapper`.

```text
product

├── controller
│   └── ProductController

├── service
│   ├── ProductService
│   ├── ProductVersionService
│   ├── ProductDocumentService
│   ├── ProductLifecycleService
│   └── ProductServiceImpl

├── repository
│   ├── InsuranceProductRepository
│   ├── InsuranceProductVersionRepository
│   └── ProductDocumentRepository

├── entity
│   ├── InsuranceProduct
│   ├── InsuranceProductVersion
│   └── ProductDocument

├── dto

├── mapper

├── validator

├── event

├── exception
```

---

# 4. Domain Entities

Primary Entities

```text
InsuranceProduct

InsuranceProductVersion

ProductDocument
```

---

# 5. Product Service

Purpose:

Manages product lifecycle.

---

## Methods

### Create Product

```java
ProductResponse createProduct(
    CreateProductRequest request
);
```

---

### Update Product

```java
ProductResponse updateProduct(
    Long productId,
    UpdateProductRequest request
);
```

---

### Get Product

```java
ProductResponse getProduct(
    Long productId
);
```

---

### Search Products

```java
Page<ProductResponse> searchProducts(
    ProductSearchRequest request
);
```

---

### Retire Product

```java
void retireProduct(
    Long productId
);
```

**As-built (2026-07-29):** No retire-product / retire-version operation shipped (no `/retire` endpoints). Activating a version implicitly expires the previously-active one; `ProductVersionStatus` still carries the lifecycle values.

---

# 6. Product Version Service

Purpose:

Manages product version lifecycle.

---

## Methods

### Create Version

```java
ProductVersionResponse createVersion(
    Long productId,
    CreateVersionRequest request
);
```

---

### Activate Version

```java
void activateVersion(
    Long productId,
    Long versionId
);
```

---

### Retire Version

```java
void retireVersion(
    Long productId,
    Long versionId
);
```

---

### Get Active Version

```java
ProductVersionResponse getActiveVersion(
    Long productId
);
```

---

### Get Product Versions

```java
List<ProductVersionResponse>
getVersions(Long productId);
```

---

# 7. Product Document Service

Purpose:

Manages product-version document associations.

---

## Methods

### Attach Document

```java
void attachDocument(
    Long versionId,
    AttachDocumentRequest request
);
```

---

### Remove Document

```java
void removeDocument(
    Long versionId,
    Long documentId
);
```

---

### Get Documents

```java
List<ProductDocumentResponse>
getDocuments(Long versionId);
```

---

# 8. Product Lifecycle Service

Purpose:

Enforces lifecycle rules.

---

## Responsibilities

```text
Activation Validation

Retirement Validation

Version Transition Rules

Document Requirements
```

---

## Lifecycle

```text
DRAFT
   ↓
ACTIVE
   ↓
EXPIRED
   ↓
RETIRED
```

---

# 9. DTO Design

## CreateProductRequest

```java
claimTypeId

productCode

productName

description
```

---

## ProductResponse

```java
id

productCode

productName

claimTypeId

status
```

---

## CreateVersionRequest

```java
versionNumber

versionName

effectiveFrom
```

---

## ProductVersionResponse

```java
versionId

versionNumber

versionName

effectiveFrom

effectiveTo

status
```

---

## AttachDocumentRequest

```java
documentId

documentType
```

---

# 10. Repository Layer

## InsuranceProductRepository

```java
extends JpaRepository<
    InsuranceProduct,
    Long
>
```

---

## Methods

```java
Optional<InsuranceProduct>
findByProductCode(
    String productCode
);
```

---

```java
Page<InsuranceProduct>
findAll(
    Specification<InsuranceProduct>,
    Pageable
);
```

---

## InsuranceProductVersionRepository

Methods:

```java
Optional<InsuranceProductVersion>
findActiveVersion(
    Long productId
);
```

---

```java
List<InsuranceProductVersion>
findByInsuranceProductId(
    Long productId
);
```

---

## ProductDocumentRepository

Methods:

```java
List<ProductDocument>
findByProductVersionId(
    Long versionId
);
```

---

# 11. Product Validation

## ProductValidator

Responsibilities:

```text
Product Validation

Code Uniqueness Validation

Claim Type Validation

Tenant Validation
```

---

Rules:

```text
Product Code Must Be Unique
Per Tenant
```

---

## ProductVersionValidator

Responsibilities:

```text
Version Validation

Activation Validation

Effective Date Validation
```

---

Rules:

```text
Only One Active Version
Per Product
```

---

## ProductDocumentValidator

Responsibilities:

```text
Document Validation

Document Type Validation
```

---

# 12. Claim Integration Service

Purpose:

Resolve product/version during claim creation.

---

## Methods

```java
resolveActiveVersion()

validateClaimProduct()

getProductForClaim()
```

---

## Workflow

```text
Claim Creation
       ↓
Selected Product
       ↓
Resolve Active Version
       ↓
Store Product ID
Store Version ID
```

---

# 13. Product Activation Workflow

```text
Validate Version
        ↓
Validate Documents
        ↓
Expire Existing Version
        ↓
Activate New Version
        ↓
Publish Event
```

---

## Mandatory Documents

Required Before Activation

```text
POLICY_WORDING
```

Optional

```text
CLAIM_GUIDELINE

COVERAGE_GUIDE

FRAUD_MANUAL

UNDERWRITING_GUIDE
```

---

# 14. Event Publishing

## ProductEventPublisher

Published Events

```text
PRODUCT_CREATED

PRODUCT_UPDATED

PRODUCT_RETIRED

PRODUCT_VERSION_CREATED

PRODUCT_VERSION_ACTIVATED

PRODUCT_VERSION_RETIRED

PRODUCT_DOCUMENT_ATTACHED

PRODUCT_DOCUMENT_REMOVED
```

---

All Events Use

```text
Outbox Pattern
```

---

# 15. Transaction Boundaries

## Create Product

```java
@Transactional
```

Workflow:

```text
Validate Request
      ↓
Create Product
      ↓
Persist Product
      ↓
Save Event
      ↓
Commit
```

---

## Activate Version

```java
@Transactional
```

Workflow:

```text
Validate Version
       ↓
Validate Documents
       ↓
Expire Current Version
       ↓
Activate New Version
       ↓
Save Event
       ↓
Commit
```

---

## Attach Document

```java
@Transactional
```

Workflow:

```text
Validate Document
       ↓
Create Association
       ↓
Save Event
       ↓
Commit
```

---

# 16. Exception Handling

Exceptions

```java
ProductNotFoundException

ProductVersionNotFoundException

DuplicateProductCodeException

ActiveVersionExistsException

MissingPolicyDocumentException

ProductValidationException
```

---

# 17. Security Rules

Required Permissions

```text
PRODUCT_CREATE

PRODUCT_VIEW

PRODUCT_UPDATE

PRODUCT_RETIRE

PRODUCT_VERSION_CREATE

PRODUCT_VERSION_ACTIVATE

PRODUCT_DOCUMENT_ATTACH
```

**As-built (2026-07-29):** Collapsed to two codes — `PRODUCT_READ` and `PRODUCT_WRITE` — enforced at the service layer via `@PreAuthorize(hasAuthority(...))`. Tenant isolation is Hibernate `@TenantId`, not a hand-written `tenant_id` filter on every query.

---

Tenant Isolation

```text
All Queries Filtered
By tenant_id
```

---

# 18. Audit Integration

Tracked Actions

```text
Product Created

Product Updated

Product Retired

Version Created

Version Activated

Document Attached

Document Removed
```

---

Every Write Operation Generates

```text
AUDIT_EVENT_CREATED
```

---

# 19. Performance Considerations

Indexes

```text
tenant_id

claim_type_id

product_code

status
```

---

Search APIs

```text
Pagination Mandatory
```

---

Active Version Lookup

Target:

```text
< 50 ms
```

---

# 20. Dependency Diagram

```text
ProductController
         ↓
ProductService
         ↓
ProductValidator
         ↓
Repositories
         ↓
PostgreSQL

ProductService
         ↓
ProductEventPublisher
         ↓
Outbox Table

ClaimService
         ↓
ProductService
         ↓
Resolve Active Version
```

---

# 21. Unit Testing Requirements

Coverage Target

```text
90%+
```

Required Tests

```text
Create Product

Update Product

Create Version

Activate Version

Retire Version

Attach Document

Resolve Active Version

Validate Claim Product
```

---

# 22. Future Enhancements

V2 Reserved

```text
Coverage Rules

Exclusion Rules

Benefit Catalog

Premium Configuration

Underwriting Rules
```

---

# Approval

This document defines the Insurance Product Module implementation blueprint and serves as the reference for product lifecycle management, version management, claim integration, document associations, event publishing, transaction management, audit integration, and future scalability.
