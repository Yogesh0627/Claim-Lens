# 07.4 User Management API

## Document Information

| Field           | Value                 |
| --------------- | --------------------- |
| Project         | ClaimLens             |
| Company         | RiskLens Technologies |
| Version         | V1                    |
| Document Type   | API Design            |
| Document Number | 07.4                  |
| Document Name   | User Management API   |
| Status          | Approved              |
| Last Updated    | June 2026             |

---

# 1. Overview

The User Management module manages all human actors within the ClaimLens platform.

Supported User Types:

```text
SYSTEM_ADMIN
TENANT_ADMIN
CLAIMS_MANAGER
INVESTIGATION_MANAGER
INVESTIGATOR
CLAIMS_ADJUSTER
CUSTOMER_SUPPORT
AUDITOR
ANALYST
```

This module provides APIs for:

* User Management
* Customer Management
* User Profile Management
* User Branch Assignment
* User Activation & Deactivation
* User Search
* User Role Assignment
* Customer Search
* Investigator Directory

---

# 2. Domain Entities

Managed Entities:

```text
user
customer
user_branch_assignment
```

Related Entities:

```text
role
permission
branch
region
insurance_company
```

---

# 3. Permission Matrix

| Permission         | Description              |
| ------------------ | ------------------------ |
| USER_VIEW          | View users               |
| USER_CREATE        | Create users             |
| USER_UPDATE        | Update users             |
| USER_DELETE        | Soft delete users        |
| USER_ACTIVATE      | Activate users           |
| USER_DEACTIVATE    | Deactivate users         |
| USER_ASSIGN_BRANCH | Assign users to branches |
| CUSTOMER_VIEW      | View customers           |
| CUSTOMER_CREATE    | Create customers         |
| CUSTOMER_UPDATE    | Update customers         |

---

# 4. User APIs

---

## Create User

### Endpoint

```http
POST /api/v1/users
```

### Permissions

```text
USER_CREATE
```

### Request Body

```json
{
  "employeeCode": "EMP0001",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@abcinsurance.com",
  "phoneNumber": "+91-9876543210",
  "roleId": 5,
  "branchIds": [301, 302]
}
```

### Validation Rules

| Field        | Rules            |
| ------------ | ---------------- |
| employeeCode | Required, Unique |
| firstName    | Required         |
| lastName     | Required         |
| email        | Required, Unique |
| phoneNumber  | Required         |
| roleId       | Must Exist       |
| branchIds    | Valid Branches   |

### Business Rules

* Email must be unique within tenant.
* Employee code must be unique within tenant.
* Role must exist and be active.
* Assigned branches must belong to tenant.

### Success Response

```http
201 Created
```

```json
{
  "userId": 1001,
  "employeeCode": "EMP0001",
  "status": "ACTIVE"
}
```

### Audit Event

```text
USER_CREATED
```

---

## Get User

### Endpoint

```http
GET /api/v1/users/{userId}
```

### Permissions

```text
USER_VIEW
```

### Success Response

```json
{
  "userId": 1001,
  "employeeCode": "EMP0001",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@abcinsurance.com",
  "role": {
    "roleId": 5,
    "roleName": "INVESTIGATOR"
  },
  "status": "ACTIVE"
}
```

---

## Update User

### Endpoint

```http
PUT /api/v1/users/{userId}
```

### Request Body

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "phoneNumber": "+91-9999999999"
}
```

### Success Response

```json
{
  "message": "User updated successfully"
}
```

### Audit Event

```text
USER_UPDATED
```

---

## Delete User

### Endpoint

```http
DELETE /api/v1/users/{userId}
```

### Description

Soft delete user.

### Success Response

```http
204 No Content
```

### Business Rules

* Active assignments must not exist.
* Open investigations must not exist.

### Audit Event

```text
USER_DELETED
```

---

## List Users

### Endpoint

```http
GET /api/v1/users
```

### Query Parameters

```text
page
size
sort
status
roleId
branchId
email
employeeCode
```

### Example

```http
GET /api/v1/users?roleId=5&status=ACTIVE
```

### Success Response

```json
{
  "content": [
    {
      "userId": 1001,
      "employeeCode": "EMP0001",
      "firstName": "John",
      "lastName": "Doe",
      "role": "INVESTIGATOR",
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

# 5. User Status Management APIs

---

## Activate User

### Endpoint

```http
PATCH /api/v1/users/{userId}/activate
```

### Permission

```text
USER_ACTIVATE
```

### Success Response

```json
{
  "status": "ACTIVE"
}
```

### Audit Event

```text
USER_ACTIVATED
```

---

## Deactivate User

### Endpoint

```http
PATCH /api/v1/users/{userId}/deactivate
```

### Permission

```text
USER_DEACTIVATE
```

### Success Response

```json
{
  "status": "INACTIVE"
}
```

### Business Rules

* User cannot deactivate themselves.
* Tenant Admin cannot be deactivated if they are the last active admin.

### Audit Event

```text
USER_DEACTIVATED
```

---

# 6. User Branch Assignment APIs

---

## Assign User To Branches

### Endpoint

```http
POST /api/v1/users/{userId}/branch-assignments
```

### Permission

```text
USER_ASSIGN_BRANCH
```

### Request Body

```json
{
  "branchIds": [301, 302]
}
```

### Success Response

```json
{
  "message": "Branch assignment completed"
}
```

### Audit Event

```text
USER_BRANCH_ASSIGNED
```

---

## Get User Branch Assignments

### Endpoint

```http
GET /api/v1/users/{userId}/branch-assignments
```

### Success Response

```json
[
  {
    "branchId": 301,
    "branchName": "Delhi Branch"
  },
  {
    "branchId": 302,
    "branchName": "Noida Branch"
  }
]
```

---

## Remove Branch Assignment

### Endpoint

```http
DELETE /api/v1/users/{userId}/branch-assignments/{branchId}
```

### Success Response

```http
204 No Content
```

### Audit Event

```text
USER_BRANCH_UNASSIGNED
```

---

# 7. Customer APIs

---

## Create Customer

### Endpoint

```http
POST /api/v1/customers
```

### Permissions

```text
CUSTOMER_CREATE
```

### Request Body

```json
{
  "customerNumber": "CUS00001",
  "firstName": "Rahul",
  "lastName": "Sharma",
  "email": "rahul@gmail.com",
  "phoneNumber": "+91-9876543210"
}
```

### Validation Rules

| Field          | Rules       |
| -------------- | ----------- |
| customerNumber | Unique      |
| firstName      | Required    |
| lastName       | Required    |
| email          | Valid Email |
| phoneNumber    | Required    |

### Success Response

```json
{
  "customerId": 5001,
  "customerNumber": "CUS00001"
}
```

### Audit Event

```text
CUSTOMER_CREATED
```

---

## Get Customer

### Endpoint

```http
GET /api/v1/customers/{customerId}
```

### Success Response

```json
{
  "customerId": 5001,
  "customerNumber": "CUS00001",
  "firstName": "Rahul",
  "lastName": "Sharma",
  "email": "rahul@gmail.com",
  "phoneNumber": "+91-9876543210"
}
```

---

## Update Customer

### Endpoint

```http
PUT /api/v1/customers/{customerId}
```

### Request Body

```json
{
  "phoneNumber": "+91-8888888888"
}
```

### Success Response

```json
{
  "message": "Customer updated successfully"
}
```

### Audit Event

```text
CUSTOMER_UPDATED
```

---

## List Customers

### Endpoint

```http
GET /api/v1/customers
```

### Query Parameters

```text
page
size
sort
customerNumber
email
phoneNumber
```

### Success Response

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

---

# 8. Search APIs

---

## Search Users

### Endpoint

```http
GET /api/v1/users/search
```

### Query Parameters

```text
q
roleId
branchId
status
```

### Example

```http
GET /api/v1/users/search?q=john
```

### Success Response

```json
[
  {
    "userId": 1001,
    "employeeCode": "EMP0001",
    "name": "John Doe",
    "role": "INVESTIGATOR"
  }
]
```

---

## Search Customers

### Endpoint

```http
GET /api/v1/customers/search
```

### Query Parameters

```text
q
```

### Example

```http
GET /api/v1/customers/search?q=rahul
```

### Success Response

```json
[
  {
    "customerId": 5001,
    "customerNumber": "CUS00001",
    "name": "Rahul Sharma"
  }
]
```

---

# 9. Investigator Directory APIs

---

## Get Available Investigators

### Endpoint

```http
GET /api/v1/users/investigators
```

### Query Parameters

```text
branchId
regionId
status
```

### Success Response

```json
[
  {
    "userId": 1001,
    "employeeCode": "EMP0001",
    "name": "John Doe",
    "activeAssignments": 12,
    "status": "ACTIVE"
  }
]
```

### Purpose

Used by:

* Assignment Engine
* Manual Assignment UI
* Workload Balancing

---

# 10. Administrative Password APIs

---

## Reset User Password

### Endpoint

```http
POST /api/v1/users/{userId}/reset-password
```

### Permission

```text
USER_UPDATE
```

### Request Body

```json
{
  "temporaryPassword": "Temp@1234"
}
```

### Success Response

```json
{
  "message": "Password reset successfully"
}
```

### Business Rules

* Force password change on next login.
* Audit mandatory.

### Audit Event

```text
USER_PASSWORD_RESET
```

---

# 11. Business Validation Rules

## User Rules

* Employee code unique within tenant.
* Email unique within tenant.
* User must belong to at least one branch.
* User role must exist and be active.
* Deleted users cannot authenticate.

## Customer Rules

* Customer number unique within tenant.
* Customer cannot be deleted if claims exist.
* Customer information is tenant-scoped.

## Branch Assignment Rules

* Branch must belong to tenant.
* User must belong to tenant.
* Duplicate assignment prohibited.

---

# 12. Error Codes

| Error Code                   | Description            |
| ---------------------------- | ---------------------- |
| USER_NOT_FOUND               | User not found         |
| CUSTOMER_NOT_FOUND           | Customer not found     |
| DUPLICATE_EMAIL              | Email already exists   |
| DUPLICATE_EMPLOYEE_CODE      | Employee code exists   |
| DUPLICATE_CUSTOMER_NUMBER    | Customer number exists |
| INVALID_ROLE                 | Role not found         |
| INVALID_BRANCH               | Branch not found       |
| BRANCH_ASSIGNMENT_EXISTS     | Assignment exists      |
| LAST_ACTIVE_ADMIN            | Cannot deactivate      |
| USER_HAS_OPEN_INVESTIGATIONS | User deletion blocked  |
| TENANT_MISMATCH              | Cross tenant access    |

---

# 13. Audit Events

```text
USER_CREATED
USER_UPDATED
USER_DELETED
USER_ACTIVATED
USER_DEACTIVATED

USER_BRANCH_ASSIGNED
USER_BRANCH_UNASSIGNED

USER_PASSWORD_RESET

CUSTOMER_CREATED
CUSTOMER_UPDATED
```

---

# 14. Performance Requirements

| API                    | Target   |
| ---------------------- | -------- |
| Create User            | < 500 ms |
| Update User            | < 300 ms |
| Search User            | < 300 ms |
| Create Customer        | < 300 ms |
| Customer Search        | < 300 ms |
| Investigator Directory | < 500 ms |

---

# 15. Security Requirements

All APIs require:

```text
JWT Authentication
Tenant Validation
Permission Validation
Audit Logging
```

Additional Controls:

* PII masking in logs
* Branch-level authorization checks
* Role escalation prevention
* Cross-tenant access prevention

---

# Approval

This document defines the User Management API contract for ClaimLens and serves as the implementation reference for user lifecycle management, customer management, branch assignments, investigator directory operations, and administrative user controls.
