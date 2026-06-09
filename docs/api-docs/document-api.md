# 07.7 Document Management API

## Document Information

| Field           | Value                   |
| --------------- | ----------------------- |
| Project         | ClaimLens               |
| Company         | Niyo Technologies   |
| Version         | V1                      |
| Document Type   | API Design              |
| Document Number | 07.7                    |
| Document Name   | Document Management API |
| Status          | Approved                |
| Last Updated    | June 2026               |

---

# 1. Overview

The Document Management module manages all claim-related documents uploaded into ClaimLens.

Key Responsibilities:

* Document Upload
* S3 Storage Integration
* Document Versioning
* Active Version Management
* Document Metadata Management
* Document Download
* Document Validation
* OCR Processing Trigger
* Analysis Trigger
* Incremental Reprocessing
* Document Lifecycle Tracking

All uploaded files are stored in AWS S3.

Database stores metadata only.

---

# 2. Domain Entities

Managed Entities:

```text
document
document_version
```

Related Entities:

```text
claim
document_type
ocr_job
analysis_job
claim_processing_state
```

---

# 3. Architecture Decisions

## Storage Strategy

Files Stored In:

```text
AWS S3
```

Metadata Stored In:

```text
PostgreSQL
```

---

## Versioning Strategy

Each upload creates:

```text
document
      ↓
document_version
```

Example:

```text
Vehicle RC

V1
V2
V3
```

Latest version becomes:

```text
ACTIVE_VERSION
```

---

## Incremental Reprocessing

When:

```text
V2 Uploaded
```

Only:

```text
V2
```

is processed.

Previous versions remain unchanged.

Fraud Engine always evaluates:

```text
Current Active Versions
```

Example:

```text
Driving License V1
Vehicle RC V2
Insurance Policy V1
```

---

# 4. Permission Matrix

| Permission        | Description        |
| ----------------- | ------------------ |
| DOCUMENT_VIEW     | View documents     |
| DOCUMENT_UPLOAD   | Upload documents   |
| DOCUMENT_UPDATE   | Update metadata    |
| DOCUMENT_DELETE   | Delete documents   |
| DOCUMENT_DOWNLOAD | Download documents |

---

# 5. Upload Flow

## Upload Architecture

```text
Frontend
      ↓
Generate Upload URL
      ↓
Pre-Signed URL
      ↓
Direct Upload To S3
      ↓
Upload Complete API
      ↓
Document Version Created
      ↓
OCR Job Queued
      ↓
Analysis Job Queued
```

---

# 6. Upload URL APIs

---

## Generate Upload URL

### Endpoint

```http
POST /api/v1/documents/upload-url
```

### Permissions

```text
DOCUMENT_UPLOAD
```

### Request Body

```json
{
  "claimId": 10001,
  "documentTypeId": 11,
  "fileName": "vehicle_rc.pdf",
  "contentType": "application/pdf"
}
```

### Validation Rules

| Field          | Rules        |
| -------------- | ------------ |
| claimId        | Must Exist   |
| documentTypeId | Must Exist   |
| fileName       | Required     |
| contentType    | Allowed Type |

### Success Response

```json
{
  "uploadUrl": "https://s3-presigned-url",
  "s3Key": "claims/10001/documents/vehicle_rc.pdf",
  "expiresIn": 300
}
```

### Audit Event

```text
DOCUMENT_UPLOAD_URL_GENERATED
```

---

## Complete Upload

### Endpoint

```http
POST /api/v1/documents/complete-upload
```

### Headers

```http
Idempotency-Key: uuid
```

### Request Body

```json
{
  "claimId": 10001,
  "documentTypeId": 11,
  "fileName": "vehicle_rc.pdf",
  "s3Key": "claims/10001/documents/vehicle_rc.pdf",
  "fileSizeBytes": 2048576
}
```

### Business Logic

System Creates:

```text
document
document_version
```

System Triggers:

```text
OCR_JOB
ANALYSIS_JOB
```

### Success Response

```json
{
  "documentId": 2001,
  "versionId": 3001,
  "versionNumber": 1
}
```

### Audit Event

```text
DOCUMENT_UPLOADED
```

---

# 7. Document APIs

---

## Get Document

### Endpoint

```http
GET /api/v1/documents/{documentId}
```

### Permissions

```text
DOCUMENT_VIEW
```

### Success Response

```json
{
  "documentId": 2001,
  "claimId": 10001,
  "documentType": "VEHICLE_RC",
  "activeVersion": 2,
  "status": "ACTIVE"
}
```

---

## List Claim Documents

### Endpoint

```http
GET /api/v1/claims/{claimId}/documents
```

### Success Response

```json
[
  {
    "documentId": 2001,
    "documentType": "VEHICLE_RC",
    "activeVersion": 2
  }
]
```

---

## Search Documents

### Endpoint

```http
GET /api/v1/documents/search
```

### Query Parameters

```text
claimId
documentTypeId
status
```

---

# 8. Document Version APIs

---

## Upload New Version

### Endpoint

```http
POST /api/v1/documents/{documentId}/versions
```

### Description

Creates a new version of an existing document.

### Request Body

```json
{
  "fileName": "vehicle_rc_updated.pdf",
  "s3Key": "claims/10001/documents/vehicle_rc_v2.pdf",
  "fileSizeBytes": 2150000
}
```

### Business Logic

System Creates:

```text
document_version
```

Updates:

```text
active_version_id
```

Triggers:

```text
OCR_JOB
ANALYSIS_JOB
```

For New Version Only.

### Success Response

```json
{
  "versionId": 3002,
  "versionNumber": 2
}
```

### Audit Event

```text
DOCUMENT_VERSION_CREATED
```

---

## Get Document Versions

### Endpoint

```http
GET /api/v1/documents/{documentId}/versions
```

### Success Response

```json
[
  {
    "versionId": 3001,
    "versionNumber": 1,
    "status": "INACTIVE"
  },
  {
    "versionId": 3002,
    "versionNumber": 2,
    "status": "ACTIVE"
  }
]
```

---

## Get Specific Version

### Endpoint

```http
GET /api/v1/documents/{documentId}/versions/{versionId}
```

### Success Response

```json
{
  "versionId": 3002,
  "versionNumber": 2,
  "fileName": "vehicle_rc_updated.pdf",
  "uploadedAt": "2026-06-01T10:00:00Z"
}
```

---

# 9. Download APIs

---

## Generate Download URL

### Endpoint

```http
GET /api/v1/documents/{documentId}/download-url
```

### Permissions

```text
DOCUMENT_DOWNLOAD
```

### Success Response

```json
{
  "downloadUrl": "https://s3-presigned-download-url",
  "expiresIn": 300
}
```

### Audit Event

```text
DOCUMENT_DOWNLOADED
```

---

## Download Specific Version

### Endpoint

```http
GET /api/v1/documents/{documentId}/versions/{versionId}/download-url
```

### Success Response

```json
{
  "downloadUrl": "https://s3-presigned-download-url",
  "expiresIn": 300
}
```

---

# 10. Metadata APIs

---

## Update Document Metadata

### Endpoint

```http
PATCH /api/v1/documents/{documentId}/metadata
```

### Request Body

```json
{
  "documentLabel": "Updated Vehicle RC"
}
```

### Success Response

```json
{
  "message": "Metadata updated successfully"
}
```

### Audit Event

```text
DOCUMENT_METADATA_UPDATED
```

---

# 11. Document Validation APIs

---

## Validate Claim Documents

### Endpoint

```http
GET /api/v1/claims/{claimId}/document-validation
```

### Purpose

Validate uploaded documents against policy requirements.

### Success Response

```json
{
  "claimId": 10001,
  "isValid": false,
  "missingDocuments": [
    {
      "documentType": "DRIVING_LICENSE"
    }
  ]
}
```

---

## Get Missing Documents

### Endpoint

```http
GET /api/v1/claims/{claimId}/missing-documents
```

### Success Response

```json
[
  {
    "documentTypeId": 11,
    "documentTypeCode": "DRIVING_LICENSE"
  }
]
```

---

# 12. Document Processing APIs

---

## Get Document Processing Status

### Endpoint

```http
GET /api/v1/documents/{documentId}/processing-status
```

### Success Response

```json
{
  "ocrStatus": "COMPLETED",
  "analysisStatus": "COMPLETED",
  "lastProcessedVersion": 2
}
```

---

## Reprocess Document Version

### Endpoint

```http
POST /api/v1/documents/{documentId}/versions/{versionId}/reprocess
```

### Permissions

```text
DOCUMENT_UPDATE
```

### Description

Force OCR and Analysis rerun.

### Success Response

```json
{
  "message": "Reprocessing initiated"
}
```

### Audit Event

```text
DOCUMENT_REPROCESS_REQUESTED
```

---

# 13. Document Lifecycle APIs

---

## Archive Document

### Endpoint

```http
PATCH /api/v1/documents/{documentId}/archive
```

### Success Response

```json
{
  "status": "ARCHIVED"
}
```

### Audit Event

```text
DOCUMENT_ARCHIVED
```

---

## Restore Document

### Endpoint

```http
PATCH /api/v1/documents/{documentId}/restore
```

### Success Response

```json
{
  "status": "ACTIVE"
}
```

### Audit Event

```text
DOCUMENT_RESTORED
```

---

## Delete Document

### Endpoint

```http
DELETE /api/v1/documents/{documentId}
```

### Description

Soft Delete Only.

### Success Response

```http
204 No Content
```

### Audit Event

```text
DOCUMENT_DELETED
```

---

# 14. Business Validation Rules

## Upload Rules

* Claim must exist.
* Document type must exist.
* File type must be allowed.
* File size must not exceed configured limits.
* Upload must complete within pre-signed URL expiration.

## Versioning Rules

* Version numbers auto-increment.
* Only one active version allowed.
* Previous versions remain immutable.

## Download Rules

* User must have document access.
* Download URL expires after 5 minutes.

## Reprocessing Rules

* Reprocessing only affects requested version.
* Reprocessing never affects historical versions.

---

# 15. Error Codes

| Error Code                   | Description            |
| ---------------------------- | ---------------------- |
| DOCUMENT_NOT_FOUND           | Document missing       |
| DOCUMENT_VERSION_NOT_FOUND   | Version missing        |
| INVALID_FILE_TYPE            | Unsupported file       |
| FILE_SIZE_EXCEEDED           | File too large         |
| DOCUMENT_UPLOAD_FAILED       | Upload failed          |
| DOCUMENT_DOWNLOAD_FAILED     | Download failed        |
| CLAIM_NOT_FOUND              | Claim missing          |
| DOCUMENT_TYPE_NOT_FOUND      | Document type missing  |
| ACTIVE_VERSION_REQUIRED      | Active version missing |
| REPROCESSING_ALREADY_RUNNING | Processing active      |

---

# 16. Audit Events

```text
DOCUMENT_UPLOAD_URL_GENERATED
DOCUMENT_UPLOADED

DOCUMENT_VERSION_CREATED

DOCUMENT_DOWNLOADED

DOCUMENT_METADATA_UPDATED

DOCUMENT_REPROCESS_REQUESTED

DOCUMENT_ARCHIVED
DOCUMENT_RESTORED
DOCUMENT_DELETED
```

---

# 17. Integration Events

Produced Events:

```text
DOCUMENT_UPLOADED
DOCUMENT_VERSION_CREATED
DOCUMENT_REPROCESSED
```

Consumed By:

```text
OCR SERVICE
ANALYSIS SERVICE
FRAUD ENGINE
NOTIFICATION SERVICE
ANALYTICS SERVICE
```

---

# 18. Performance Requirements

| API                 | Target   |
| ------------------- | -------- |
| Generate Upload URL | < 200 ms |
| Complete Upload     | < 500 ms |
| Document Lookup     | < 200 ms |
| Document Validation | < 300 ms |
| Download URL        | < 200 ms |
| Version History     | < 300 ms |

---

# 19. Security Requirements

All APIs require:

```text
JWT Authentication
Tenant Validation
Permission Validation
Audit Logging
```

Additional Controls:

* S3 access only through pre-signed URLs.
* Direct bucket access prohibited.
* File type validation mandatory.
* Malware scanning required before processing.
* Cross-tenant document access prohibited.

---

# Approval

This document defines the Document Management API contract for ClaimLens and serves as the implementation reference for document uploads, S3 integration, versioning, active version management, validation, downloads, OCR integration, analysis integration, and incremental reprocessing workflows.
