> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; where it diverges from the shipped code the authoritative sources win: [`../domain-model.md`](../domain-model.md), [`../architecture.md`](../architecture.md), [`../audit-report.md`](../audit-report.md), and the running API. Concrete divergences are flagged inline as **As-built** notes.

# 07.3 Organization Management API

## Document Information

| Field           | Value                       |
| --------------- | --------------------------- |
| Project         | ClaimLens                   |
| Company         | Niyo Technologies       |
| Version         | V1                          |
| Document Type   | API Design                  |
| Document Number | 07.3                        |
| Document Name   | Organization Management API |
| Status          | Approved                    |
| Last Updated    | June 2026                   |

---

# 1. Overview

The Organization Management module manages the hierarchical structure of insurance companies within ClaimLens.

Organization Hierarchy:

```text
Insurance Company
        ↓
      Region
        ↓
      Branch
```

This module provides APIs for:

* Insurance Company Management
* Region Management
* Branch Management
* Organization Hierarchy Retrieval
* Branch Assignment Support
* Search & Filtering

**As-built (2026-07-29):** several cross-cutting facts differ from this doc:
> - **Tenant comes from the JWT** — there is **no `{companyId}` path variable** anywhere. Region/branch requests do not carry a `companyId` in body or path; the tenant is resolved server-side from the token.
> - **Branches are nested under region**: `POST/GET/PUT/DELETE /organizations/regions/{rid}/branches[/{bid}]`, plus a flat list `GET /organizations/branches`. There is no top-level `POST /organizations/branches`.
> - **Company lookups** use `GET /organizations/companies/me` (the caller's own tenant) and `GET /organizations/companies/{id}`; there is **no `/hierarchy` and no `/search` endpoint** (sections 7 are not built).
> - The module also ships **Departments** (`/organizations/departments`) and **Designations** (`/organizations/designations`) — not covered by this design doc.
> - Cross-tenant access returns **404**, not 403 (see section 12 correction).

---

# 2. Domain Entities

Managed Entities:

```text
insurance_company
region
branch
```

Related Tables:

```text
insurance_company
region
branch
user_branch_assignment
```

---

# 3. Permission Matrix

| Permission          | Description                       |
| ------------------- | --------------------------------- |
| ORGANIZATION_VIEW   | View organization hierarchy       |
| ORGANIZATION_CREATE | Create company, region, branch    |
| ORGANIZATION_UPDATE | Update organization entities      |
| ORGANIZATION_DELETE | Soft delete organization entities |

**As-built (2026-07-29):** these coarse `ORGANIZATION_*` permissions do not exist. Authorization is **per entity type**: `ORG_COMPANY_{READ,WRITE}`, `ORG_REGION_{READ,WRITE}`, `ORG_BRANCH_{READ,WRITE}`, `ORG_DEPARTMENT_{READ,WRITE}`, `ORG_DESIGNATION_{READ,WRITE}`. Additionally, **company create/list/delete require `PLATFORM_ADMIN`** (tenant provisioning), while company read/update use `ORG_COMPANY_READ`/`ORG_COMPANY_WRITE`.

---

# 4. Insurance Company APIs

---

## Create Insurance Company

### Endpoint

```http
POST /api/v1/organizations/companies
```

### Description

Create a new insurance company tenant.

### Permissions

```text
ORGANIZATION_CREATE
```

**As-built (2026-07-29):** `POST /organizations/companies` requires **`PLATFORM_ADMIN`** (this is tenant provisioning), not `ORGANIZATION_CREATE`.

### Request Body

```json
{
  "companyName": "ABC Insurance Ltd",
  "companyCode": "ABCINS",
  "email": "admin@abcinsurance.com",
  "phoneNumber": "+91-9876543210",
  "website": "https://abcinsurance.com"
}
```

### Validation Rules

| Field       | Rules             |
| ----------- | ----------------- |
| companyName | Required, Max 200 |
| companyCode | Required, Unique  |
| email       | Valid Email       |
| website     | Valid URL         |

### Success Response

```http
201 Created
```

```json
{
  "companyId": 101,
  "companyCode": "ABCINS",
  "status": "ACTIVE"
}
```

### Audit Event

```text
INSURANCE_COMPANY_CREATED
```

---

## Get Insurance Company

### Endpoint

```http
GET /api/v1/organizations/companies/{companyId}
```

**As-built (2026-07-29):** the path variable is `{id}` (`GET /organizations/companies/{id}`, `ORG_COMPANY_READ`). To fetch the caller's own tenant without knowing its id, use **`GET /organizations/companies/me`** (`ORG_COMPANY_READ`).

### Permissions

```text
ORGANIZATION_VIEW
```

### Success Response

```json
{
  "companyId": 101,
  "companyName": "ABC Insurance Ltd",
  "companyCode": "ABCINS",
  "email": "admin@abcinsurance.com",
  "phoneNumber": "+91-9876543210",
  "status": "ACTIVE",
  "createdAt": "2026-06-01T10:00:00Z"
}
```

---

## Update Insurance Company

### Endpoint

```http
PUT /api/v1/organizations/companies/{companyId}
```

**As-built (2026-07-29):** path is `{id}`; requires `ORG_COMPANY_WRITE`.

### Request Body

```json
{
  "companyName": "ABC General Insurance Ltd",
  "email": "support@abcinsurance.com",
  "phoneNumber": "+91-9876543211"
}
```

### Success Response

```json
{
  "message": "Insurance company updated successfully"
}
```

### Audit Event

```text
INSURANCE_COMPANY_UPDATED
```

---

## Delete Insurance Company

### Endpoint

```http
DELETE /api/v1/organizations/companies/{companyId}
```

**As-built (2026-07-29):** path is `{id}`; requires **`PLATFORM_ADMIN`**.

### Description

Soft delete insurance company.

### Success Response

```http
204 No Content
```

### Audit Event

```text
INSURANCE_COMPANY_DELETED
```

---

## List Insurance Companies

### Endpoint

```http
GET /api/v1/organizations/companies
```

**As-built (2026-07-29):** listing all companies requires **`PLATFORM_ADMIN`** (a tenant admin sees only their own tenant via `/companies/me`).

### Query Parameters

```text
?page=0
&size=20
&sort=companyName,asc
&status=ACTIVE
```

### Success Response

```json
{
  "content": [
    {
      "companyId": 101,
      "companyName": "ABC Insurance Ltd",
      "companyCode": "ABCINS",
      "status": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

# 5. Region APIs

---

## Create Region

### Endpoint

```http
POST /api/v1/organizations/regions
```

**As-built (2026-07-29):** requires `ORG_REGION_WRITE`. The request body has **no `companyId`** — the tenant is taken from the JWT.

### Request Body

```json
{
  "companyId": 101,
  "regionName": "North Region",
  "regionCode": "NORTH"
}
```

### Validation Rules

| Field      | Rules                 |
| ---------- | --------------------- |
| companyId  | Must Exist            |
| regionName | Required              |
| regionCode | Unique Within Company |

### Success Response

```json
{
  "regionId": 201,
  "regionCode": "NORTH",
  "status": "ACTIVE"
}
```

### Audit Event

```text
REGION_CREATED
```

---

## Get Region

### Endpoint

```http
GET /api/v1/organizations/regions/{regionId}
```

### Success Response

```json
{
  "regionId": 201,
  "companyId": 101,
  "regionName": "North Region",
  "regionCode": "NORTH",
  "status": "ACTIVE"
}
```

---

## Update Region

### Endpoint

```http
PUT /api/v1/organizations/regions/{regionId}
```

### Request Body

```json
{
  "regionName": "Northern Region"
}
```

### Success Response

```json
{
  "message": "Region updated successfully"
}
```

### Audit Event

```text
REGION_UPDATED
```

---

## Delete Region

### Endpoint

```http
DELETE /api/v1/organizations/regions/{regionId}
```

### Success Response

```http
204 No Content
```

### Audit Event

```text
REGION_DELETED
```

---

## List Regions

### Endpoint

```http
GET /api/v1/organizations/regions
```

**As-built (2026-07-29):** `ORG_REGION_READ`; the list is already tenant-scoped from the JWT, so there is **no `companyId` filter**.

### Query Parameters

```text
companyId
status
page
size
sort
```

### Example

```http
GET /api/v1/organizations/regions?companyId=101
```

---

# 6. Branch APIs

---

## Create Branch

### Endpoint

```http
POST /api/v1/organizations/branches
```

**As-built (2026-07-29):** branches are created **nested under their region**: `POST /organizations/regions/{rid}/branches` (`ORG_BRANCH_WRITE`), with the region id in the path (not a `regionId` field). The same nesting applies to get/update/delete: `/organizations/regions/{rid}/branches/{bid}`.

### Request Body

```json
{
  "regionId": 201,
  "branchName": "Delhi Branch",
  "branchCode": "DEL001",
  "city": "Delhi",
  "state": "Delhi",
  "address": "Connaught Place"
}
```

### Validation Rules

| Field      | Rules                |
| ---------- | -------------------- |
| regionId   | Must Exist           |
| branchName | Required             |
| branchCode | Unique Within Region |
| city       | Required             |
| state      | Required             |

### Success Response

```json
{
  "branchId": 301,
  "branchCode": "DEL001",
  "status": "ACTIVE"
}
```

### Audit Event

```text
BRANCH_CREATED
```

---

## Get Branch

### Endpoint

```http
GET /api/v1/organizations/branches/{branchId}
```

**As-built (2026-07-29):** a single branch is fetched via its region: `GET /organizations/regions/{rid}/branches/{bid}` (`ORG_BRANCH_READ`).

### Success Response

```json
{
  "branchId": 301,
  "regionId": 201,
  "branchName": "Delhi Branch",
  "branchCode": "DEL001",
  "city": "Delhi",
  "state": "Delhi",
  "status": "ACTIVE"
}
```

---

## Update Branch

### Endpoint

```http
PUT /api/v1/organizations/branches/{branchId}
```

### Request Body

```json
{
  "branchName": "Delhi Central Branch",
  "address": "New Connaught Place"
}
```

### Success Response

```json
{
  "message": "Branch updated successfully"
}
```

### Audit Event

```text
BRANCH_UPDATED
```

---

## Delete Branch

### Endpoint

```http
DELETE /api/v1/organizations/branches/{branchId}
```

### Success Response

```http
204 No Content
```

### Audit Event

```text
BRANCH_DELETED
```

---

## List Branches

### Endpoint

```http
GET /api/v1/organizations/branches
```

**As-built (2026-07-29):** this flat tenant-wide branch list **is** built (`ORG_BRANCH_READ`) and is the one branch endpoint not nested under a region. It returns all branches for the caller's tenant (no `companyId` filter — tenant is from the JWT).

### Query Parameters

```text
regionId
companyId
city
state
status
page
size
sort
```

### Example

```http
GET /api/v1/organizations/branches?regionId=201
```

---

# 7. Organization Hierarchy APIs

**As-built (2026-07-29):** not built. There is **no `/organizations/companies/{companyId}/hierarchy`** endpoint and **no `/organizations/search`** endpoint. Clients assemble hierarchy from the separate region/branch/department/designation list endpoints.

---

## Get Company Hierarchy

### Endpoint

```http
GET /api/v1/organizations/companies/{companyId}/hierarchy
```

### Description

Retrieve complete hierarchy including regions and branches.

### Success Response

```json
{
  "companyId": 101,
  "companyName": "ABC Insurance Ltd",
  "regions": [
    {
      "regionId": 201,
      "regionName": "North Region",
      "branches": [
        {
          "branchId": 301,
          "branchName": "Delhi Branch"
        }
      ]
    }
  ]
}
```

---

## Search Organization

### Endpoint

```http
GET /api/v1/organizations/search
```

### Query Parameters

```text
q
entityType
```

### Example

```http
GET /api/v1/organizations/search?q=Delhi
```

### Success Response

```json
[
  {
    "entityType": "BRANCH",
    "entityId": 301,
    "name": "Delhi Branch"
  }
]
```

---

# 8. Business Validation Rules

## Company Rules

* Company Code must be unique.
* Company Name cannot be duplicated within tenant.
* Deleted companies cannot be modified.

## Region Rules

* Region Code unique within company.
* Region must belong to tenant.
* Region cannot be deleted if active branches exist.

## Branch Rules

* Branch Code unique within region.
* Branch must belong to tenant.
* Branch cannot be deleted if active users are assigned.

---

# 9. Error Codes

| Error Code                 | Description              |
| -------------------------- | ------------------------ |
| COMPANY_NOT_FOUND          | Company not found        |
| REGION_NOT_FOUND           | Region not found         |
| BRANCH_NOT_FOUND           | Branch not found         |
| DUPLICATE_COMPANY_CODE     | Company code exists      |
| DUPLICATE_REGION_CODE      | Region code exists       |
| DUPLICATE_BRANCH_CODE      | Branch code exists       |
| REGION_HAS_ACTIVE_BRANCHES | Region cannot be deleted |
| BRANCH_HAS_ACTIVE_USERS    | Branch cannot be deleted |
| TENANT_MISMATCH            | Cross tenant access      |

---

# 10. Audit Events

Generated Events:

```text
INSURANCE_COMPANY_CREATED
INSURANCE_COMPANY_UPDATED
INSURANCE_COMPANY_DELETED

REGION_CREATED
REGION_UPDATED
REGION_DELETED

BRANCH_CREATED
BRANCH_UPDATED
BRANCH_DELETED
```

---

# 11. Performance Requirements

| API            | Target   |
| -------------- | -------- |
| Create Company | < 500 ms |
| Create Region  | < 300 ms |
| Create Branch  | < 300 ms |
| Hierarchy API  | < 800 ms |
| Search API     | < 300 ms |
| List APIs      | < 500 ms |

---

# 12. Security Requirements

All endpoints require:

```text
JWT Authentication
Tenant Validation
Permission Validation
Audit Logging
```

Organization data must always be tenant scoped.

Cross-tenant access is prohibited and must return:

```http
403 Forbidden
```

**As-built (2026-07-29):** cross-tenant access by direct id returns **404 Not Found**, not 403 — the tenant discriminator hides the row entirely, so there is no existence oracle. (403 is reserved for an authenticated caller lacking the required permission on an in-tenant resource.)

---

# Approval

This document defines the Organization Management API contract for ClaimLens and serves as the implementation reference for Insurance Company, Region, Branch, and Organization Hierarchy management.
