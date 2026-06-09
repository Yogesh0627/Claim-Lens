# 15.3 API Design – Policy Intelligence Domain

Status: Approved

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

This document defines APIs for:

* Policy Intelligence
* Policy Copilot
* Coverage Validation
* Scenario Analysis
* Investigation Copilot
* Policy Knowledge Management
* AI Auditability

These APIs expose the RAG capabilities of ClaimLens.

---

# API Standards

Base Path

```http
/api/v1/policy-intelligence
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
All requests scoped by tenant_id
```

---

# High-Level Workflow

```text
Insurance Product Version
        ↓
Policy Documents
        ↓
Chunking
        ↓
Embeddings
        ↓
Knowledge Base
        ↓
User Questions
        ↓
Vector Search
        ↓
Gemini
        ↓
Grounded Answer
```

---

# Policy Knowledge APIs

These APIs manage policy ingestion and indexing.

---

## Get Knowledge Base Status

Returns indexing status for a product version.

### Endpoint

```http
GET /api/v1/policy-intelligence/products/{productVersionId}/status
```

### Response

```json
{
  "productVersionId": 501,
  "status": "READY",
  "totalDocuments": 5,
  "totalChunks": 845,
  "totalEmbeddings": 845
}
```

---

## Rebuild Knowledge Base

Forces re-indexing.

### Endpoint

```http
POST /api/v1/policy-intelligence/products/{productVersionId}/rebuild
```

### Response

```json
{
  "jobId": 1001,
  "status": "QUEUED"
}
```

### Permissions

```text
POLICY_AI_ADMIN
```

---

## Get Knowledge Statistics

### Endpoint

```http
GET /api/v1/policy-intelligence/products/{productVersionId}/statistics
```

### Response

```json
{
  "documents": 5,
  "chunks": 845,
  "embeddings": 845
}
```

---

# Chat Session APIs

---

## Create Chat Session

Creates a new AI conversation.

### Endpoint

```http
POST /api/v1/policy-intelligence/sessions
```

### Request

```json
{
  "productVersionId": 501,
  "sessionName": "Coverage Questions"
}
```

### Response

```json
{
  "sessionId": 2001
}
```

---

## Get Chat Sessions

### Endpoint

```http
GET /api/v1/policy-intelligence/sessions
```

### Response

```json
[
  {
    "sessionId": 2001,
    "sessionName": "Coverage Questions"
  }
]
```

---

## Get Session Details

### Endpoint

```http
GET /api/v1/policy-intelligence/sessions/{sessionId}
```

### Response

```json
{
  "sessionId": 2001,
  "sessionName": "Coverage Questions",
  "createdAt": "2026-07-01T10:00:00Z"
}
```

---

## Get Session Messages

### Endpoint

```http
GET /api/v1/policy-intelligence/sessions/{sessionId}/messages
```

---

# Question APIs

---

## Ask Policy Question

General-purpose policy Q&A.

### Endpoint

```http
POST /api/v1/policy-intelligence/questions
```

### Request

```json
{
  "sessionId": 2001,
  "question": "Is windshield damage covered?"
}
```

### Response

```json
{
  "questionId": 3001,
  "answerId": 4001,
  "answer": "Yes. Windshield damage is covered under the comprehensive plan.",
  "citations": [
    {
      "document": "Policy Wording",
      "section": "5.2"
    }
  ]
}
```

---

## Get Question History

### Endpoint

```http
GET /api/v1/policy-intelligence/questions
```

### Filters

```text
productVersionId

sessionId

createdBy

dateFrom

dateTo
```

---

## Get Question Details

### Endpoint

```http
GET /api/v1/policy-intelligence/questions/{questionId}
```

---

# Coverage Validation APIs

Specialized policy coverage endpoint.

---

## Validate Coverage

### Endpoint

```http
POST /api/v1/policy-intelligence/coverage-validation
```

### Request

```json
{
  "productVersionId": 501,
  "scenario": "Vehicle damaged during flooding."
}
```

### Response

```json
{
  "coverageAvailable": true,
  "answer": "Flood damage is covered under comprehensive coverage.",
  "citations": [
    {
      "document": "Policy Wording",
      "section": "7.3"
    }
  ]
}
```

---

# Scenario Analysis APIs

Used for hypothetical situations.

---

## Analyze Scenario

### Endpoint

```http
POST /api/v1/policy-intelligence/scenarios
```

### Request

```json
{
  "productVersionId": 501,
  "scenario": "My son was driving the vehicle and met with an accident."
}
```

### Response

```json
{
  "analysis": "Coverage may apply depending on driver eligibility conditions.",
  "citations": [
    {
      "document": "Policy Wording",
      "section": "3.1"
    }
  ]
}
```

---

# Investigation Copilot APIs

Provides investigation assistance.

---

## Investigate Policy Clause

### Endpoint

```http
POST /api/v1/policy-intelligence/investigation-copilot
```

### Request

```json
{
  "claimId": 1001,
  "question": "Show exclusions related to alcohol usage."
}
```

### Response

```json
{
  "answer": "Claims involving intoxicated driving may be excluded.",
  "citations": [
    {
      "document": "Policy Wording",
      "section": "11.4"
    }
  ]
}
```

---

# Citation APIs

---

## Get Answer Citations

### Endpoint

```http
GET /api/v1/policy-intelligence/answers/{answerId}/citations
```

### Response

```json
[
  {
    "document": "Policy Wording",
    "section": "5.2",
    "clauseReference": "EP-001"
  }
]
```

---

# AI Audit APIs

---

## Get AI Interaction Log

### Endpoint

```http
GET /api/v1/policy-intelligence/audit/interactions
```

### Filters

```text
sessionId

questionId

productVersionId

createdBy

dateFrom

dateTo
```

---

## Get AI Interaction Details

### Endpoint

```http
GET /api/v1/policy-intelligence/audit/interactions/{interactionId}
```

### Response

```json
{
  "question": "...",
  "answer": "...",
  "model": "gemini-2.5",
  "confidence": 92.5,
  "retrievalTimeMs": 80,
  "generationTimeMs": 1400
}
```

---

# Administrative APIs

---

## Re-Generate Embeddings

### Endpoint

```http
POST /api/v1/policy-intelligence/admin/embeddings/regenerate
```

### Permissions

```text
POLICY_AI_ADMIN
```

---

## Get Embedding Statistics

### Endpoint

```http
GET /api/v1/policy-intelligence/admin/embeddings/statistics
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
  "errorCode": "POLICY_AI_ERROR",
  "message": "Unable to process request"
}
```

---

# Error Codes

```text
KNOWLEDGE_BASE_NOT_READY

PRODUCT_VERSION_NOT_FOUND

SESSION_NOT_FOUND

QUESTION_NOT_FOUND

ANSWER_NOT_FOUND

EMBEDDING_GENERATION_FAILED

VECTOR_SEARCH_FAILED

LLM_RESPONSE_FAILED

POLICY_AI_PERMISSION_DENIED
```

---

# Security Model

Permissions

```text
POLICY_AI_VIEW

POLICY_AI_ASK

POLICY_AI_AUDIT_VIEW

POLICY_AI_ADMIN
```

---

# AI Rules

All responses must:

```text
Use Retrieved Chunks Only

Include Citations

Respect Product Version

Be Auditable
```

---

Responses must never:

```text
Approve Claims

Reject Claims

Determine Fraud

Make Legal Decisions
```

---

# Events Published

```text
POLICY_CHAT_SESSION_CREATED

POLICY_QUESTION_ASKED

POLICY_ANSWER_GENERATED

POLICY_CITATION_CREATED

POLICY_KNOWLEDGE_REBUILT

POLICY_EMBEDDINGS_GENERATED
```

---

# API Summary

Knowledge APIs

```text
Knowledge Status

Knowledge Statistics

Knowledge Rebuild
```

---

Chat APIs

```text
Create Session

Get Sessions

Get Messages
```

---

Question APIs

```text
Ask Question

Get History

Get Details
```

---

Copilot APIs

```text
Coverage Validation

Scenario Analysis

Investigation Copilot
```

---

Audit APIs

```text
Interaction Logs

Citation Retrieval

AI Traceability
```

---

Enterprise Policy Intelligence

```text
SUPPORTED
```
