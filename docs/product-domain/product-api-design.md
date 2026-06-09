# 14.3 API Design – Insurance Product Domain

Status: Approved

Version: 1.0

Owner: RiskLens Technologies

---

# Purpose

This document defines APIs for managing:

* Insurance Products
* Product Versions
* Product Documents
* Product Lifecycle
* Product Version Activation

The APIs provide the foundation for:

* Product Administration
* Policy Version Management
* Claim Product Selection
* Future Policy Intelligence Integration

---

# API Standards

Base Path

```http
/api/v1/products
```

Authentication

```text
JWT Required
```

Authorization

```text
Role Based Access Control (RBAC)
```

Tenant Isolation

```text
Every request must be scoped by tenant_id
```

---

# Product Lifecycle

```text
Create Product
       ↓
Create Version
       ↓
Upload Documents
       ↓
Activate Version
       ↓
Claims Use Active Version
       ↓
Retire Version
```

---

# Product APIs

---

## Create Product

Creates a new insurance product.

### Endpoint

```http
POST /api/v1/products
```

### Request

```json
{
  "claimTypeId": 1,
  "productCode": "PRIVATE_CAR_PREMIUM",
  "productName": "Private Car Premium",
  "description": "Premium comprehensive motor insurance"
}
```

### Response

```json
{
  "id": 101,
  "productCode": "PRIVATE_CAR_PREMIUM",
  "productName": "Private Car Premium",
  "status": "DRAFT"
}
```

### Permissions

```text
PRODUCT_CREATE
```

---

## Get Product

### Endpoint

```http
GET /api/v1/products/{productId}
```

### Response

```json
{
  "id": 101,
  "productCode": "PRIVATE_CAR_PREMIUM",
  "productName": "Private Car Premium",
  "claimType": "MOTOR",
  "status": "ACTIVE"
}
```

### Permissions

```text
PRODUCT_VIEW
```

---

## Search Products

### Endpoint

```http
GET /api/v1/products
```

### Query Parameters

```text
claimTypeId

status

productCode

productName

page

size
```

### Example

```http
GET /api/v1/products?status=ACTIVE&page=0&size=20
```

---

## Update Product

### Endpoint

```http
PUT /api/v1/products/{productId}
```

### Request

```json
{
  "productName": "Private Car Premium Plus",
  "description": "Updated description"
}
```

### Permissions

```text
PRODUCT_UPDATE
```

---

## Retire Product

### Endpoint

```http
POST /api/v1/products/{productId}/retire
```

### Response

```json
{
  "success": true
}
```

### Permissions

```text
PRODUCT_RETIRE
```

---

# Product Version APIs

---

## Create Product Version

Creates a new version of a product.

### Endpoint

```http
POST /api/v1/products/{productId}/versions
```

### Request

```json
{
  "versionNumber": 2,
  "versionName": "Private Car Premium V2",
  "effectiveFrom": "2026-07-01"
}
```

### Response

```json
{
  "versionId": 501,
  "status": "DRAFT"
}
```

### Permissions

```text
PRODUCT_VERSION_CREATE
```

---

## Get Product Versions

### Endpoint

```http
GET /api/v1/products/{productId}/versions
```

### Response

```json
[
  {
    "id": 500,
    "versionNumber": 1,
    "status": "EXPIRED"
  },
  {
    "id": 501,
    "versionNumber": 2,
    "status": "ACTIVE"
  }
]
```

---

## Get Product Version

### Endpoint

```http
GET /api/v1/products/{productId}/versions/{versionId}
```

---

## Activate Product Version

### Endpoint

```http
POST /api/v1/products/{productId}/versions/{versionId}/activate
```

### Workflow

```text
Validate Version
       ↓
Expire Existing Active Version
       ↓
Activate Selected Version
       ↓
Publish Event
```

### Response

```json
{
  "success": true,
  "status": "ACTIVE"
}
```

### Permissions

```text
PRODUCT_VERSION_ACTIVATE
```

---

## Retire Product Version

### Endpoint

```http
POST /api/v1/products/{productId}/versions/{versionId}/retire
```

### Permissions

```text
PRODUCT_VERSION_RETIRE
```

---

# Product Document APIs

---

## Attach Product Document

Associates a document with a product version.

### Endpoint

```http
POST /api/v1/products/{productId}/versions/{versionId}/documents
```

### Request

```json
{
  "documentId": 2001,
  "documentType": "POLICY_WORDING"
}
```

### Response

```json
{
  "id": 7001
}
```

### Permissions

```text
PRODUCT_DOCUMENT_ATTACH
```

---

## Get Product Documents

### Endpoint

```http
GET /api/v1/products/{productId}/versions/{versionId}/documents
```

### Response

```json
[
  {
    "id": 7001,
    "documentType": "POLICY_WORDING"
  },
  {
    "id": 7002,
    "documentType": "COVERAGE_GUIDE"
  }
]
```

---

## Remove Product Document

### Endpoint

```http
DELETE /api/v1/products/{productId}/versions/{versionId}/documents/{documentId}
```

### Permissions

```text
PRODUCT_DOCUMENT_REMOVE
```

---

# Claim Integration APIs

These APIs support claim creation workflows.

---

## Get Active Product Version

### Endpoint

```http
GET /api/v1/products/{productId}/active-version
```

### Response

```json
{
  "versionId": 501,
  "versionNumber": 2,
  "status": "ACTIVE"
}
```

---

## Get Products By Claim Type

### Endpoint

```http
GET /api/v1/products/claim-types/{claimTypeId}
```

### Response

```json
[
  {
    "id": 101,
    "productName": "Private Car Premium"
  },
  {
    "id": 102,
    "productName": "Private Car Basic"
  }
]
```

Purpose:

```text
Claim Creation Dropdown
```

---

## Validate Product Selection

### Endpoint

```http
POST /api/v1/products/validate
```

### Request

```json
{
  "productId": 101,
  "versionId": 501
}
```

### Response

```json
{
  "valid": true
}
```

---

# Standard Response Structure

Success

```json
{
  "success": true,
  "data": {}
}
```

---

Failure

```json
{
  "success": false,
  "errorCode": "PRODUCT_NOT_FOUND",
  "message": "Product not found"
}
```

---

# Error Codes

```text
PRODUCT_NOT_FOUND

PRODUCT_VERSION_NOT_FOUND

PRODUCT_ALREADY_EXISTS

ACTIVE_VERSION_EXISTS

INVALID_VERSION_STATUS

PRODUCT_DOCUMENT_NOT_FOUND

PRODUCT_VALIDATION_FAILED
```

---

# Security Model

Permissions

```text
PRODUCT_CREATE

PRODUCT_VIEW

PRODUCT_UPDATE

PRODUCT_RETIRE

PRODUCT_VERSION_CREATE

PRODUCT_VERSION_ACTIVATE

PRODUCT_VERSION_RETIRE

PRODUCT_DOCUMENT_ATTACH

PRODUCT_DOCUMENT_REMOVE
```

---

# Business Rules

## Rule 1

Only one ACTIVE version allowed.

```text
VALID
```

```text
V1 EXPIRED

V2 ACTIVE

V3 DRAFT
```

---

## Rule 2

Claims must store:

```text
insuranceProductId

insuranceProductVersionId
```

---

## Rule 3

Retired versions cannot become active.

---

## Rule 4

Version cannot be activated without:

```text
POLICY_WORDING
```

document attached.

---

# Events Published

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

# API Summary

Product APIs

```text
Create Product

Update Product

Search Product

Retire Product
```

Version APIs

```text
Create Version

Activate Version

Retire Version

Get Versions
```

Document APIs

```text
Attach Document

Remove Document

List Documents
```

Claim Integration APIs

```text
Get Active Version

Validate Product

Get Products By Claim Type
```

Enterprise Insurance Product Management

```text
SUPPORTED
```
