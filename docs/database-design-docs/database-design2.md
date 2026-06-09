# Database Design Part 3 - Access Control Domain

Status: Draft

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* role
* permission
* role_permission

These tables implement the Role-Based Access Control (RBAC) system used throughout ClaimLens.

---

# RBAC Design Principles

ClaimLens follows:

One User
↓
One Role
↓
Many Permissions

Users are assigned exactly one role.

Roles are assigned multiple permissions.

Permissions control access to system capabilities.

---

# Table: role

Purpose:

Represents a business responsibility.

Examples:

* COMPANY_ADMIN
* REGIONAL_ADMIN
* INVESTIGATION_MANAGER
* INVESTIGATOR
* EMPLOYEE

Schema:

public

---

Columns

| Column         | Type         | Constraints   |
| -------------- | ------------ | ------------- |
| id             | BIGSERIAL    | PRIMARY KEY   |
| code           | VARCHAR(100) | NOT NULL      |
| name           | VARCHAR(255) | NOT NULL      |
| description    | TEXT         |               |
| is_system_role | BOOLEAN      | DEFAULT TRUE  |
| status         | VARCHAR(50)  | NOT NULL      |
| created_at     | TIMESTAMP    | NOT NULL      |
| created_by     | BIGINT       |               |
| updated_at     | TIMESTAMP    |               |
| updated_by     | BIGINT       |               |
| is_deleted     | BOOLEAN      | DEFAULT FALSE |
| deleted_at     | TIMESTAMP    |               |
| deleted_by     | BIGINT       |               |

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_role_code
ON role(code);
```

---

Indexes

```sql
CREATE INDEX idx_role_status
ON role(status);
```

---

Status Values

```text
ACTIVE

INACTIVE
```

---

System Roles

```text
COMPANY_ADMIN

REGIONAL_ADMIN

INVESTIGATION_MANAGER

INVESTIGATOR

EMPLOYEE
```

---

# Table: permission

Purpose:

Represents a single system capability.

Examples:

* CREATE_CLAIM
* VIEW_CLAIM
* UPDATE_CLAIM
* APPROVE_CLAIM
* MANAGE_USERS

Schema:

public

---

Columns

| Column      | Type         | Constraints |
| ----------- | ------------ | ----------- |
| id          | BIGSERIAL    | PRIMARY KEY |
| code        | VARCHAR(150) | NOT NULL    |
| name        | VARCHAR(255) | NOT NULL    |
| module      | VARCHAR(100) | NOT NULL    |
| description | TEXT         |             |
| created_at  | TIMESTAMP    | NOT NULL    |

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_permission_code
ON permission(code);
```

---

Indexes

```sql
CREATE INDEX idx_permission_module
ON permission(module);
```

---

Suggested Modules

```text
AUTH

USER

CUSTOMER

CLAIM

DOCUMENT

ASSIGNMENT

INVESTIGATION

FRAUD

NOTIFICATION

ANALYTICS

AUDIT

SYSTEM
```

---

Sample Permissions

```text
CREATE_USER
VIEW_USER
UPDATE_USER
DELETE_USER

CREATE_CLAIM
VIEW_CLAIM
UPDATE_CLAIM

ASSIGN_CLAIM

APPROVE_CLAIM
REJECT_CLAIM

VIEW_ANALYTICS

VIEW_AUDIT_LOGS
```

---

# Table: role_permission

Purpose:

Maps roles to permissions.

Relationship:

Role
↓
Many Permissions

Permission
↓
Many Roles

Schema:

public

---

Columns

| Column        | Type      | Constraints |
| ------------- | --------- | ----------- |
| id            | BIGSERIAL | PRIMARY KEY |
| role_id       | BIGINT    | NOT NULL    |
| permission_id | BIGINT    | NOT NULL    |
| created_at    | TIMESTAMP | NOT NULL    |
| created_by    | BIGINT    |             |

---

Foreign Keys

```sql
ALTER TABLE role_permission
ADD CONSTRAINT fk_role_permission_role
FOREIGN KEY (role_id)
REFERENCES role(id);

ALTER TABLE role_permission
ADD CONSTRAINT fk_role_permission_permission
FOREIGN KEY (permission_id)
REFERENCES permission(id);
```

---

Unique Constraints

Prevent duplicate mappings.

```sql
CREATE UNIQUE INDEX uq_role_permission
ON role_permission(role_id, permission_id);
```

---

Indexes

```sql
CREATE INDEX idx_role_permission_role
ON role_permission(role_id);

CREATE INDEX idx_role_permission_permission
ON role_permission(permission_id);
```

---

# Authorization Flow

User
↓
Role
↓
RolePermission
↓
Permission

Example:

INVESTIGATOR

↓

Permissions

* VIEW_CLAIM
* UPDATE_CLAIM
* REQUEST_DOCUMENTS
* CREATE_INVESTIGATION_NOTE

---

# Permission Evaluation Strategy

Authentication

↓

JWT Token

↓

Load User

↓

Load Role

↓

Load Permissions

↓

Cache In Redis

↓

Authorize Request

---

# Redis Authorization Cache

Key Format

```text
role_permissions:{role_id}
```

Example

```text
role_permissions:4
```

Contains:

```json
[
  "VIEW_CLAIM",
  "UPDATE_CLAIM",
  "CREATE_INVESTIGATION_NOTE"
]
```

Benefits:

* Faster Authorization
* Reduced Database Queries

---

# Role Management Rules

System Roles

```text
COMPANY_ADMIN

REGIONAL_ADMIN

INVESTIGATION_MANAGER

INVESTIGATOR

EMPLOYEE
```

Cannot be deleted.

Can be deactivated.

---

Custom Roles

Future Enhancement

V2 feature.

Not required in V1.

---

# Access Control ERD

Role

```text
id (PK)
```

↓

RolePermission

```text
role_id (FK)

permission_id (FK)
```

↓

Permission

```text
id (PK)
```

---

# Query Optimization Strategy

Common Queries

```sql
SELECT *
FROM role_permission
WHERE role_id = ?;
```

```sql
SELECT *
FROM permission
WHERE module = ?;
```

Indexes support these access patterns.

---

# Access Control Domain Summary

Tables

```text
role

permission

role_permission
```

Relationships

```text
Role
    ↔
Permission
```

Pattern

```text
Many-to-Many
```

Authorization

```text
RBAC
```

Caching

```text
Redis
```

Future Ready

```text
Custom Roles
```



# Database Design Part 4 - User Domain

Status: Draft

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

This section defines the physical PostgreSQL design for:

* user
* customer
* user_branch_assignment

These tables manage platform users, customers, authentication, and investigator branch assignments.

---

# User Domain Design Principles

Internal Users

* Company Admin
* Regional Admin
* Investigation Manager
* Investigator
* Employee

Stored in:

```text
user
```

---

External Claimants

Stored in:

```text
customer
```

---

Reason

Customers and Employees have fundamentally different lifecycles, permissions, authentication requirements, and business responsibilities.

Therefore:

```text
User
≠
Customer
```

---

# Table: user

Purpose:

Represents an internal platform user.

Schema:

public

---

Columns

| Column              | Type         | Constraints   |
| ------------------- | ------------ | ------------- |
| id                  | BIGSERIAL    | PRIMARY KEY   |
| tenant_id           | BIGINT       | NOT NULL      |
| role_id             | BIGINT       | NOT NULL      |
| employee_code       | VARCHAR(100) | NOT NULL      |
| first_name          | VARCHAR(100) | NOT NULL      |
| last_name           | VARCHAR(100) | NOT NULL      |
| email               | VARCHAR(255) | NOT NULL      |
| phone_number        | VARCHAR(30)  |               |
| password_hash       | TEXT         | NOT NULL      |
| region_id           | BIGINT       |               |
| home_branch_id      | BIGINT       |               |
| designation         | VARCHAR(150) |               |
| joining_date        | DATE         |               |
| profile_picture_url | TEXT         |               |
| last_login_at       | TIMESTAMP    |               |
| status              | VARCHAR(50)  | NOT NULL      |
| created_at          | TIMESTAMP    | NOT NULL      |
| created_by          | BIGINT       |               |
| updated_at          | TIMESTAMP    |               |
| updated_by          | BIGINT       |               |
| is_deleted          | BOOLEAN      | DEFAULT FALSE |
| deleted_at          | TIMESTAMP    |               |
| deleted_by          | BIGINT       |               |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)

role_id
    → role(id)

region_id
    → region(id)

home_branch_id
    → branch(id)
```

---

Unique Constraints

Email unique inside tenant.

```sql
CREATE UNIQUE INDEX uq_user_tenant_email
ON "user"(tenant_id, email);
```

Employee code unique inside tenant.

```sql
CREATE UNIQUE INDEX uq_user_tenant_employee_code
ON "user"(tenant_id, employee_code);
```

---

Indexes

```sql
CREATE INDEX idx_user_tenant
ON "user"(tenant_id);

CREATE INDEX idx_user_role
ON "user"(role_id);

CREATE INDEX idx_user_region
ON "user"(region_id);

CREATE INDEX idx_user_branch
ON "user"(home_branch_id);

CREATE INDEX idx_user_status
ON "user"(status);
```

---

Status Values

```text
ACTIVE

INACTIVE

SUSPENDED

LOCKED
```

---

Employee Code Format

Examples

```text
HDFC_ADMIN_00001

HDFC_MGR_00001

HDFC_INV_00001

HDFC_EMP_00001
```

Generated automatically.

---

User Scope Rules

Company Admin

```text
region_id = NULL

home_branch_id = NULL
```

---

Regional Admin

```text
region_id = REQUIRED

home_branch_id = NULL
```

---

Investigator

```text
region_id = REQUIRED

home_branch_id = REQUIRED
```

---

Employee

```text
region_id = REQUIRED

home_branch_id = REQUIRED
```

---

# Table: customer

Purpose:

Represents an insurance claimant.

Schema:

public

---

Columns

| Column          | Type         | Constraints   |
| --------------- | ------------ | ------------- |
| id              | BIGSERIAL    | PRIMARY KEY   |
| tenant_id       | BIGINT       | NOT NULL      |
| customer_number | VARCHAR(100) | NOT NULL      |
| first_name      | VARCHAR(100) | NOT NULL      |
| last_name       | VARCHAR(100) | NOT NULL      |
| email           | VARCHAR(255) |               |
| phone_number    | VARCHAR(30)  | NOT NULL      |
| date_of_birth   | DATE         |               |
| address         | TEXT         |               |
| city            | VARCHAR(100) |               |
| state           | VARCHAR(100) |               |
| country         | VARCHAR(100) |               |
| postal_code     | VARCHAR(20)  |               |
| status          | VARCHAR(50)  | NOT NULL      |
| created_at      | TIMESTAMP    | NOT NULL      |
| created_by      | BIGINT       |               |
| updated_at      | TIMESTAMP    |               |
| updated_by      | BIGINT       |               |
| is_deleted      | BOOLEAN      | DEFAULT FALSE |
| deleted_at      | TIMESTAMP    |               |
| deleted_by      | BIGINT       |               |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)
```

---

Unique Constraints

```sql
CREATE UNIQUE INDEX uq_customer_tenant_number
ON customer(tenant_id, customer_number);
```

---

Indexes

```sql
CREATE INDEX idx_customer_tenant
ON customer(tenant_id);

CREATE INDEX idx_customer_phone
ON customer(phone_number);

CREATE INDEX idx_customer_status
ON customer(status);
```

---

Status Values

```text
ACTIVE

INACTIVE

BLACKLISTED
```

---

Customer Number Format

Examples

```text
HDFC_CUST_000001

ICICI_CUST_000001
```

Generated automatically.

---

# Table: user_branch_assignment

Purpose:

Supports investigator assignments across multiple branches.

Reason:

An investigator may belong to:

```text
Primary Branch

+
Secondary Branches

+
Temporary Branches
```

This creates a many-to-many relationship.

---

Schema:

public

---

Columns

| Column          | Type        | Constraints |
| --------------- | ----------- | ----------- |
| id              | BIGSERIAL   | PRIMARY KEY |
| tenant_id       | BIGINT      | NOT NULL    |
| user_id         | BIGINT      | NOT NULL    |
| branch_id       | BIGINT      | NOT NULL    |
| assignment_type | VARCHAR(50) | NOT NULL    |
| status          | VARCHAR(50) | NOT NULL    |
| start_date      | DATE        |             |
| end_date        | DATE        |             |
| created_at      | TIMESTAMP   | NOT NULL    |
| created_by      | BIGINT      |             |

---

Foreign Keys

```sql
tenant_id
    → insurance_company(id)

user_id
    → user(id)

branch_id
    → branch(id)
```

---

Unique Constraints

Prevent duplicate branch assignment.

```sql
CREATE UNIQUE INDEX uq_user_branch
ON user_branch_assignment(user_id, branch_id);
```

---

Indexes

```sql
CREATE INDEX idx_user_branch_user
ON user_branch_assignment(user_id);

CREATE INDEX idx_user_branch_branch
ON user_branch_assignment(branch_id);

CREATE INDEX idx_user_branch_tenant
ON user_branch_assignment(tenant_id);
```

---

Assignment Types

```text
PRIMARY

SECONDARY

TEMPORARY
```

---

Status Values

```text
ACTIVE

INACTIVE
```

---

Business Rules

Every Investigator must have:

```text
One PRIMARY Branch
```

Optional:

```text
Many SECONDARY Branches

Many TEMPORARY Branches
```

---

# Authentication Strategy

Password Storage

```text
BCrypt
```

Never store plain text passwords.

---

JWT Authentication

Access Token

```text
15 Minutes
```

Refresh Token

```text
7 Days
```

Stored separately.

---

# User Domain ERD

InsuranceCompany

```text
id (PK)
```

↓

User

```text
tenant_id (FK)
role_id (FK)
region_id (FK)
home_branch_id (FK)
```

↓

UserBranchAssignment

```text
user_id (FK)

branch_id (FK)
```

---

InsuranceCompany

```text
id (PK)
```

↓

Customer

```text
tenant_id (FK)
```

---

# Query Optimization Strategy

Most Common Queries

```sql
WHERE tenant_id = ?

WHERE role_id = ?

WHERE home_branch_id = ?

WHERE status = ?
```

Indexes added accordingly.

---

# User Domain Summary

Tables

```text
user

customer

user_branch_assignment
```

Relationships

```text
User
    → Role

User
    → Region

User
    → Branch

User
    ↔ Branch

Customer
    → Tenant
```

Authentication

```text
JWT

BCrypt
```

Authorization

```text
RBAC
```

Multi-Tenancy

```text
tenant_id
```

Fully Supported.
