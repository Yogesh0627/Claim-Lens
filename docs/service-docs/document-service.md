# 09.3 Document Service Design

## Document Information

| Field           | Value                   |
| --------------- | ----------------------- |
| Project         | ClaimLens               |
| Company         | Niyo Technologies   |
| Version         | V1                      |
| Document Type   | Service Design          |
| Document Number | 09.3                    |
| Document Name   | Document Service Design |
| Status          | Approved                |
| Last Updated    | June 2026               |

---

# 1. Overview

The Document Module is responsible for managing all claim-related documents.

Responsibilities:

* Document Upload
* Document Versioning
* Active Version Management
* Document Validation
* Document Metadata Management
* S3 Integration
* Incremental Reprocessing Initiation
* Processing Workflow Initiation

The Document Module serves as the entry point into the OCR and Analysis pipeline.

---

# 2. Module Dependencies

## Incoming Dependencies

Consumed Events:

```text
INFORMATION_REQUEST_RESPONDED
```

---

## Outgoing Dependencies

Published Events:

```text
DOCUMENT_UPLOADED

DOCUMENT_VERSION_CREATED

DOCUMENT_VALIDATED

DOCUMENT_VERSION_ACTIVATED

DOCUMENT_VERSION_ARCHIVED

DOCUMENT_DELETED

DOCUMENT_RESTORED
```

---

# 3. Package Structure

```text
document

├── controller
│   └── DocumentController

├── service
│   ├── DocumentService
│   ├── DocumentVersionService
│   ├── S3StorageService
│   └── DocumentServiceImpl

├── repository
│   ├── DocumentRepository
│   └── DocumentVersionRepository

├── entity
│   ├── Document
│   └── DocumentVersion

├── dto
│   ├── request
│   └── response

├── mapper

├── validator

├── event

├── exception

├── storage

├── specification
```

---

# 4. Domain Entities

Primary Aggregate:

```text
Document
```

Child Entity:

```text
DocumentVersion
```

Relationship:

```text
Document
    └── DocumentVersion (1:N)
```

---

# 5. Controller Layer

## DocumentController

Base Path

```http
/api/v1/documents
```

---

## Endpoints

```http
POST   /documents/upload

POST   /documents/{documentId}/versions

GET    /documents/{documentId}

GET    /documents/{documentId}/versions

GET    /documents/{documentId}/versions/{versionId}

PATCH  /documents/{documentId}/versions/{versionId}/activate

DELETE /documents/{documentId}

PATCH  /documents/{documentId}/restore

GET    /documents/claims/{claimId}
```

---

# 6. Service Layer

## DocumentService

```java
public interface DocumentService
```

---

## Methods

### Upload New Document

```java
DocumentResponse uploadDocument(
    UploadDocumentRequest request
);
```

---

### Upload New Version

```java
DocumentVersionResponse uploadVersion(
    Long documentId,
    UploadVersionRequest request
);
```

---

### Get Document

```java
DocumentResponse getDocument(
    Long documentId
);
```

---

### Get Claim Documents

```java
List<DocumentResponse> getClaimDocuments(
    Long claimId
);
```

---

### Delete Document

```java
void deleteDocument(
    Long documentId
);
```

---

### Restore Document

```java
void restoreDocument(
    Long documentId
);
```

---

### Activate Version

```java
void activateVersion(
    Long documentId,
    Long versionId
);
```

---

# 7. Supporting Services

## S3StorageService

Responsibilities:

```text
Upload Files

Generate Object Keys

Generate Presigned URLs

Delete Files

File Metadata Retrieval
```

---

## DocumentVersionService

Responsibilities:

```text
Version Creation

Version Activation

Version Archiving

Version Validation
```

---

# 8. DTO Design

## UploadDocumentRequest

```java
tenantId

claimId

documentTypeId

MultipartFile file
```

---

## UploadVersionRequest

```java
MultipartFile file

versionComment
```

---

## DocumentResponse

```java
documentId

claimId

documentType

activeVersionId

createdAt
```

---

## DocumentVersionResponse

```java
versionId

versionNumber

storagePath

fileName

fileSize

isActive

uploadedAt
```

---

# 9. Repository Layer

## DocumentRepository

```java
extends JpaRepository<Document, Long>
```

---

## Methods

```java
List<Document>
findByClaimId(Long claimId);
```

---

```java
Optional<Document>
findByIdAndTenantId(
    Long documentId,
    Long tenantId
);
```

---

## DocumentVersionRepository

```java
extends JpaRepository<DocumentVersion, Long>
```

---

## Methods

```java
List<DocumentVersion>
findByDocumentIdOrderByVersionNumberDesc(
    Long documentId
);
```

---

```java
Optional<DocumentVersion>
findByDocumentIdAndIsActiveTrue(
    Long documentId
);
```

---

# 10. Validation Layer

## DocumentValidator

Responsibilities:

```text
Document Type Validation

Claim Validation

Tenant Validation

File Validation

Version Validation
```

---

## File Validation

Allowed Types:

```text
PDF

PNG

JPEG

JPG
```

---

## File Size

```text
Maximum 25 MB
```

---

## Version Validation

```text
Document Must Exist

Document Must Belong To Claim

Version Must Be New
```

---

# 11. Storage Strategy

## S3 Object Structure

```text
tenant/{tenantId}/
    claims/{claimId}/
        documents/{documentId}/
            versions/{versionId}/
                file.pdf
```

---

## Database Stores

```text
storage_path

file_name

file_size

content_type

checksum
```

---

## Database Does NOT Store

```text
File Content

OCR Payloads

Images
```

---

# 12. Version Management Strategy

## Initial Upload

```text
Document
    ↓
Version 1
```

---

## New Upload

```text
Version 1
     ↓
Version 2
```

---

## Rules

```text
One Active Version Only

Unlimited Historical Versions

History Never Deleted
```

---

# 13. Active Version Workflow

```text
Version 1 Active
       ↓
Upload Version 2
       ↓
Archive Version 1
       ↓
Activate Version 2
       ↓
Publish Events
```

---

## Transaction Flow

```text
Archive Old Version
       ↓
Activate New Version
       ↓
Save Event
       ↓
Commit
```

---

# 14. Event Publishing

## DocumentEventPublisher

Published Events:

```text
DOCUMENT_UPLOADED

DOCUMENT_VERSION_CREATED

DOCUMENT_VALIDATED

DOCUMENT_VERSION_ACTIVATED

DOCUMENT_VERSION_ARCHIVED

DOCUMENT_DELETED

DOCUMENT_RESTORED
```

---

## Outbox Rule

All events stored in:

```text
audit.outbox_event
```

before publishing.

---

# 15. Incremental Reprocessing Strategy

When:

```text
Version 2 Uploaded
```

Current Snapshot:

```text
DL V1

RC V1

POLICY V1
```

After Upload:

```text
DL V1

RC V2

POLICY V1
```

---

## Processing Rule

Reprocess:

```text
RC V2 ONLY
```

Do Not Reprocess:

```text
DL V1

POLICY V1
```

---

# 16. Transaction Boundaries

## Upload Document

```java
@Transactional
```

Workflow:

```text
Validate File
      ↓
Upload To S3
      ↓
Persist Document
      ↓
Persist Version
      ↓
Save Outbox Event
      ↓
Commit
```

---

## Upload Version

```java
@Transactional
```

Workflow:

```text
Validate
      ↓
Upload To S3
      ↓
Archive Previous Version
      ↓
Create New Version
      ↓
Activate New Version
      ↓
Save Events
      ↓
Commit
```

---

# 17. Exception Handling

Exceptions:

```java
DocumentNotFoundException

VersionNotFoundException

InvalidDocumentTypeException

FileSizeExceededException

StorageUploadException
```

---

# 18. Security Rules

Required Permissions:

```text
DOCUMENT_UPLOAD

DOCUMENT_VIEW

DOCUMENT_DELETE

DOCUMENT_RESTORE
```

---

Tenant Isolation:

```text
Every Query Filtered
By tenant_id
```

---

# 19. Audit Integration

Tracked Actions:

```text
Document Uploaded

Version Created

Version Activated

Version Archived

Document Deleted

Document Restored
```

---

Every action generates:

```text
AUDIT_EVENT_CREATED
```

---

# 20. Performance Considerations

Indexes:

```text
claim_id

document_type_id

tenant_id

is_active
```

---

S3 Upload Strategy:

```text
Direct Server Upload (V1)
```

Future:

```text
Presigned Upload URLs
```

---

# 21. Dependency Diagram

```text
DocumentController
          ↓
DocumentService
          ↓
DocumentValidator
          ↓
DocumentRepository
          ↓
PostgreSQL

DocumentService
          ↓
S3StorageService
          ↓
AWS S3

DocumentService
          ↓
DocumentEventPublisher
          ↓
Outbox Table
```

---

# 22. Unit Testing Requirements

Coverage Target:

```text
90%+
```

Required Tests:

```text
Document Upload

Version Upload

Version Activation

Version Archiving

Delete Document

Restore Document

File Validation

S3 Upload Failure Handling
```

---

# 23. Future Enhancements

V2 Reserved:

```text
Document Classification

Duplicate Detection

Tampering Detection

Digital Signature Verification

Virus Scanning Service
```

---

# Approval

This document defines the Document Module implementation blueprint and serves as the reference for document management, version control, S3 storage integration, incremental reprocessing, validation, event publishing, transaction management, audit integration, and future scalability.
