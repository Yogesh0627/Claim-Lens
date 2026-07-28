> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; where it diverges from the shipped code the authoritative sources win: [`../domain-model.md`](../domain-model.md), [`../architecture.md`](../architecture.md), [`../audit-report.md`](../audit-report.md), and the running API. Concrete divergences are flagged inline as **As-built** notes.

# 07.2 Authentication & Authorization API

## Document Information

| Field           | Value                              |
| --------------- | ---------------------------------- |
| Project         | ClaimLens                          |
| Company         | Niyo Technologies              |
| Version         | V1                                 |
| Document Type   | API Design                         |
| Document Number | 07.2                               |
| Document Name   | Authentication & Authorization API |
| Status          | Approved                           |
| Last Updated    | June 2026                          |

---

# 1. Overview

The Authentication & Authorization module is responsible for:

* User Authentication
* JWT Token Management
* Refresh Token Management
* Password Management
* Session Management
* Role-Based Access Control (RBAC)
* Permission Validation
* Login Auditing
* Security Event Tracking

All protected APIs in ClaimLens depend on this module.

---

# 2. Authentication Architecture

Authentication Mechanism:

```text
JWT Access Token + Refresh Token
```

Authentication Flow:

```text
User Login
      ↓
Access Token Issued
      ↓
Refresh Token Issued
      ↓
API Access
      ↓
Access Token Expired
      ↓
Refresh Token Used
      ↓
New Access Token Issued
```

**As-built (2026-07-29):** The complete shipped auth surface (base `/api/v1`) is: `POST /auth/login`, `POST /auth/google` (Google sign-in — not in the original design; disabled when client-id is blank), `POST /auth/refresh-token`, `POST /auth/logout`, `POST /auth/set-password` (used for both invitation acceptance and password reset — there is no `/auth/reset-password` or `/auth/validate-reset-token`), `POST /auth/forgot-password`, and `GET /auth/me`. All are `permitAll` except `/auth/me` (resolved from the principal). Change-password lives under the profile module: `POST /api/v1/profile/change-password` (`isAuthenticated`), NOT `/auth/change-password`. The `/auth/sessions*` and `/auth/permissions` endpoints were not built.

---

# 3. Access Token Specification

Type:

```text
JWT
```

Expiration:

```text
15 Minutes
```

Claims:

```json
{
  "sub": "1001",
  "tenantId": 101,
  "employeeCode": "EMP0001",
  "roleId": 5,
  "permissions": [
    "CLAIM_VIEW",
    "CLAIM_CREATE"
  ]
}
```

**As-built (2026-07-29):** The JWT carries `{sub, tid, rid}` — tenant id (`tid`) and role id (`rid`), and **roleId, NOT a permissions array**. Permissions are resolved server-side per request from `role_permission` (plus per-tenant `tenant_role_permission` overrides) and cached. HS256 with the algorithm pinned on parse; the secret has no default (fail-fast). Access-token TTL is a config value.

---

# 4. Refresh Token Specification

Storage:

```text
Database
```

Table:

```text
user_session
```

Expiration:

```text
7 Days
```

Rotation:

```text
Enabled
```

Each refresh generates:

* New Access Token
* New Refresh Token

Old refresh token becomes invalid.

---

# 5. Authorization Model

ClaimLens uses:

```text
Role Based Access Control (RBAC)
```

Entities:

* Role
* Permission
* Role Permission Mapping
* User Role Assignment

Authorization Check:

```text
JWT Validation
      ↓
Permission Validation
      ↓
Tenant Validation
      ↓
Endpoint Access
```

---

# 6. Common Headers

Required Headers:

```http
Authorization: Bearer <jwt>
X-Tenant-Id: 101
```

Optional:

```http
X-Correlation-Id
```

---

# 7. Login API

## Endpoint

```http
POST /api/v1/auth/login
```

---

### Description

Authenticate user and generate tokens.

---

### Authentication

Not Required

---

### Request Body

```json
{
  "email": "investigator@abcinsurance.com",
  "password": "SecurePassword123"
}
```

---

### Validation Rules

| Field    | Rule             |
| -------- | ---------------- |
| email    | Required         |
| email    | Valid Email      |
| password | Required         |
| password | Min 8 Characters |

---

### Success Response

```http
200 OK
```

```json
{
  "accessToken": "jwt-token",
  "refreshToken": "refresh-token",
  "expiresIn": 900,
  "user": {
    "userId": 101,
    "employeeCode": "EMP0001",
    "firstName": "John",
    "lastName": "Doe",
    "role": "INVESTIGATOR"
  }
}
```

---

### Failure Responses

```http
401 Unauthorized
```

```json
{
  "errorCode": "INVALID_CREDENTIALS",
  "message": "Invalid username or password"
}
```

---

### Audit Event

```text
USER_LOGIN
```

---

# 8. Refresh Token API

## Endpoint

```http
POST /api/v1/auth/refresh-token
```

---

### Description

Generate new access token.

---

### Request Body

```json
{
  "refreshToken": "refresh-token"
}
```

---

### Success Response

```json
{
  "accessToken": "new-access-token",
  "refreshToken": "new-refresh-token",
  "expiresIn": 900
}
```

---

### Failure Responses

```http
401 Unauthorized
```

```json
{
  "errorCode": "INVALID_REFRESH_TOKEN",
  "message": "Refresh token is invalid"
}
```

---

### Audit Event

```text
TOKEN_REFRESHED
```

---

# 9. Logout API

## Endpoint

```http
POST /api/v1/auth/logout
```

---

### Description

Terminate active session.

---

### Authentication

Required

---

### Request Body

```json
{
  "refreshToken": "refresh-token"
}
```

---

### Success Response

```http
204 No Content
```

---

### Actions Performed

* Refresh token revoked
* Session invalidated
* Logout audit recorded

---

### Audit Event

```text
USER_LOGOUT
```

---

# 10. Forgot Password API

## Endpoint

```http
POST /api/v1/auth/forgot-password
```

---

### Description

Generate password reset request.

---

### Request Body

```json
{
  "email": "investigator@company.com"
}
```

---

### Success Response

```json
{
  "message": "Password reset link sent"
}
```

---

### Security Rule

Always return:

```http
200 OK
```

to prevent account enumeration.

---

### Audit Event

```text
PASSWORD_RESET_REQUESTED
```

---

# 11. Validate Reset Token API

**As-built (2026-07-29):** Not built — there is no `/auth/validate-reset-token` endpoint.

## Endpoint

```http
POST /api/v1/auth/validate-reset-token
```

---

### Request Body

```json
{
  "token": "reset-token"
}
```

---

### Success Response

```json
{
  "valid": true
}
```

---

### Failure Response

```json
{
  "valid": false
}
```

---

# 12. Reset Password API

**As-built (2026-07-29):** The reset flow completes via `POST /api/v1/auth/set-password` (single-use, SHA-256-hashed token; TTL 1h reset / 7d invite) — there is no `/auth/reset-password`. The enforced minimum password length is **6 characters** (known gap), not 8, and the composition rules below are not enforced.

## Endpoint

```http
POST /api/v1/auth/reset-password
```

---

### Request Body

```json
{
  "token": "reset-token",
  "newPassword": "NewPassword123",
  "confirmPassword": "NewPassword123"
}
```

---

### Validation Rules

Password Policy:

Minimum:

```text
8 Characters
```

Must contain:

* Uppercase Letter
* Lowercase Letter
* Number
* Special Character

---

### Success Response

```json
{
  "message": "Password updated successfully"
}
```

---

### Audit Event

```text
PASSWORD_RESET_COMPLETED
```

---

# 13. Change Password API

**As-built (2026-07-29):** The path is `POST /api/v1/profile/change-password` (`isAuthenticated`), NOT `/auth/change-password`. It does **not** revoke existing sessions (known gap documented in the audit report) — see the note under Security Actions.

## Endpoint

```http
POST /api/v1/auth/change-password
```

---

### Authentication

Required

---

### Request Body

```json
{
  "currentPassword": "OldPassword",
  "newPassword": "NewPassword123",
  "confirmPassword": "NewPassword123"
}
```

---

### Success Response

```json
{
  "message": "Password changed successfully"
}
```

---

### Security Actions

* Revoke existing sessions
* Generate audit event

---

### Audit Event

```text
PASSWORD_CHANGED
```

---

# 14. Current User API

## Endpoint

```http
GET /api/v1/auth/me
```

---

### Description

Return authenticated user profile.

---

### Authentication

Required

---

### Success Response

```json
{
  "userId": 101,
  "employeeCode": "EMP0001",
  "email": "john@company.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "INVESTIGATOR",
  "permissions": [
    "CLAIM_VIEW",
    "CLAIM_CREATE"
  ]
}
```

---

# 15. Active Sessions API

**As-built (2026-07-29):** Not built — there is no session-listing or session-revoke API (sections 15–16), and no `/auth/permissions` endpoint (section 17). Refresh tokens are single-use and rotated on use, but there is no user-facing session management surface.

## Endpoint

```http
GET /api/v1/auth/sessions
```

---

### Description

Retrieve active sessions for current user.

---

### Success Response

```json
[
  {
    "sessionId": 1001,
    "device": "Chrome Windows",
    "ipAddress": "192.168.1.10",
    "loginTime": "2026-06-01T12:00:00Z",
    "lastActivity": "2026-06-01T13:00:00Z"
  }
]
```

---

# 16. Revoke Session API

## Endpoint

```http
DELETE /api/v1/auth/sessions/{sessionId}
```

---

### Description

Terminate a specific session.

---

### Success Response

```http
204 No Content
```

---

### Audit Event

```text
SESSION_REVOKED
```

---

# 17. Permission Validation API

## Endpoint

```http
GET /api/v1/auth/permissions
```

---

### Description

Returns all permissions assigned to current user.

---

### Success Response

```json
{
  "role": "INVESTIGATOR",
  "permissions": [
    "CLAIM_VIEW",
    "CLAIM_CREATE",
    "DOCUMENT_UPLOAD",
    "INVESTIGATION_CREATE"
  ]
}
```

---

# 18. Authentication Error Codes

| Error Code            | Description           |
| --------------------- | --------------------- |
| INVALID_CREDENTIALS   | Invalid login         |
| ACCOUNT_DISABLED      | User disabled         |
| ACCOUNT_LOCKED        | Account locked        |
| PASSWORD_EXPIRED      | Password expired      |
| TOKEN_EXPIRED         | JWT expired           |
| INVALID_TOKEN         | Invalid JWT           |
| INVALID_REFRESH_TOKEN | Invalid refresh token |
| RESET_TOKEN_EXPIRED   | Reset token expired   |
| SESSION_NOT_FOUND     | Session not found     |
| ACCESS_DENIED         | Missing permission    |

---

# 19. Account Lockout Policy

**As-built (2026-07-29):** No count-based account lockout was built. Brute-force protection is instead rate-limiting: an `auth` bucket of 20 requests/min per IP per URI (fail-open, keyed on leftmost `X-Forwarded-For` — a known spoofing gap).

Failed Login Threshold:

```text
5 Attempts
```

Lock Duration:

```text
30 Minutes
```

Audit Event:

```text
ACCOUNT_LOCKED
```

---

# 20. Security Controls

Implemented Controls:

* JWT Authentication
* Refresh Token Rotation
* Password Hashing (BCrypt)
* Account Lockout
* Session Tracking
* Password Complexity Rules
* Audit Logging
* Tenant Isolation
* Permission-Based Authorization

---

# 21. Permission Matrix

| Permission           | Description           |
| -------------------- | --------------------- |
| USER_VIEW            | View Users            |
| USER_CREATE          | Create Users          |
| USER_UPDATE          | Update Users          |
| USER_DELETE          | Delete Users          |
| ROLE_VIEW            | View Roles            |
| ROLE_MANAGE          | Manage Roles          |
| CLAIM_VIEW           | View Claims           |
| CLAIM_CREATE         | Create Claims         |
| CLAIM_UPDATE         | Update Claims         |
| DOCUMENT_UPLOAD      | Upload Documents      |
| INVESTIGATION_CREATE | Create Investigations |
| FRAUD_VIEW           | View Fraud Results    |
| AUDIT_VIEW           | View Audit Logs       |
| DASHBOARD_VIEW       | View Dashboards       |

---

# 22. Audit Events

Authentication module generates:

```text
USER_LOGIN
USER_LOGOUT
TOKEN_REFRESHED
PASSWORD_RESET_REQUESTED
PASSWORD_RESET_COMPLETED
PASSWORD_CHANGED
ACCOUNT_LOCKED
SESSION_REVOKED
ACCESS_DENIED
```

---

# 23. Performance Requirements

| Metric                    | Target   |
| ------------------------- | -------- |
| Login API                 | < 500 ms |
| Refresh Token API         | < 300 ms |
| Logout API                | < 200 ms |
| Password Change API       | < 500 ms |
| Permission Validation API | < 200 ms |

---

# Approval

This document defines the authentication and authorization API contract for ClaimLens and serves as the implementation reference for Spring Security, JWT handling, RBAC enforcement, and session management.
