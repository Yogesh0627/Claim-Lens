# 15.4 Policy Intelligence Service Design

## Document Information

| Field           | Value                              |
| --------------- | ---------------------------------- |
| Project         | ClaimLens                          |
| Company         | RiskLens Technologies              |
| Version         | V1                                 |
| Document Type   | Service Design                     |
| Document Number | 15.4                               |
| Document Name   | Policy Intelligence Service Design |
| Status          | Approved                           |
| Last Updated    | June 2026                          |

---

# 1. Overview

The Policy Intelligence Module provides Retrieval Augmented Generation (RAG) capabilities for insurance products and policy documents.

Responsibilities:

* Policy Knowledge Ingestion
* Document Chunking
* Embedding Generation
* Vector Search
* Answer Generation
* Coverage Validation
* Scenario Analysis
* Investigation Copilot
* Citation Generation
* AI Auditability

The module transforms insurance documents into an enterprise-grade knowledge system.

---

# 2. Module Dependencies

## Consumed Events

```text
PRODUCT_DOCUMENT_ATTACHED

DOCUMENT_UPLOADED

DOCUMENT_VERSION_CREATED

OCR_COMPLETED
```

---

## Published Events

```text
POLICY_CHUNK_CREATED

POLICY_EMBEDDINGS_GENERATED

POLICY_KNOWLEDGE_READY

POLICY_CHAT_SESSION_CREATED

POLICY_QUESTION_ASKED

POLICY_ANSWER_GENERATED

POLICY_CITATION_CREATED

POLICY_KNOWLEDGE_REBUILT
```

---

# 3. Package Structure

```text
policyintelligence

├── controller
│   └── PolicyIntelligenceController

├── service
│   ├── PolicyIngestionService
│   ├── ChunkingService
│   ├── EmbeddingService
│   ├── VectorSearchService
│   ├── AnswerGenerationService
│   ├── PolicyCopilotService
│   ├── CoverageValidationService
│   ├── InvestigationCopilotService
│   └── PolicyIntelligenceServiceImpl

├── repository
│   ├── PolicyChunkRepository
│   ├── PolicyEmbeddingRepository
│   ├── PolicyQuestionRepository
│   ├── PolicyAnswerRepository
│   └── CoverageQuestionLogRepository

├── entity
│   ├── PolicyChunk
│   ├── PolicyEmbedding
│   ├── PolicyQuestion
│   ├── PolicyAnswer
│   └── CoverageQuestionLog

├── worker

├── event

├── validator

├── exception
```

---

# 4. Architecture Overview

```text
Product Document
        ↓
OCR
        ↓
PolicyIngestionService
        ↓
ChunkingService
        ↓
EmbeddingService
        ↓
pgvector
        ↓
VectorSearchService
        ↓
Gemini
        ↓
AnswerGenerationService
        ↓
User
```

---

# 5. Policy Ingestion Service

Purpose:

Converts policy documents into searchable knowledge.

Responsibilities:

```text
Document Processing

Chunk Creation

Knowledge Registration

Rebuild Triggering
```

---

## Methods

```java
ingestDocument()

rebuildKnowledgeBase()

processProductVersion()
```

---

# 6. Chunking Service

Purpose:

Converts policy text into retrieval chunks.

Responsibilities:

```text
Section Detection

Clause Detection

Chunk Generation

Metadata Extraction
```

---

## Chunking Strategy

Target Size

```text
500-1000 Tokens
```

---

Overlap

```text
100-150 Tokens
```

---

Chunk Metadata

```text
Document

Section

Clause

Page Number
```

---

## Methods

```java
createChunks()

splitDocument()

extractMetadata()
```

---

# 7. Embedding Service

Purpose:

Generates vector embeddings.

Responsibilities:

```text
Embedding Generation

Embedding Persistence

Embedding Regeneration
```

---

## Provider

V1

```text
Gemini Embedding Model
```

---

## Methods

```java
generateEmbedding()

generateEmbeddings()

regenerateEmbeddings()
```

---

# 8. Vector Search Service

Purpose:

Retrieves relevant policy knowledge.

Responsibilities:

```text
Similarity Search

Version Filtering

Context Retrieval
```

---

## Workflow

```text
Question
      ↓
Embedding
      ↓
Vector Search
      ↓
Top 10 Chunks
```

---

## Critical Rule

Every query must filter by:

```text
product_version_id
```

---

## Methods

```java
searchRelevantChunks()

searchByVersion()

rankResults()
```

---

# 9. Answer Generation Service

Purpose:

Generates grounded answers.

Responsibilities:

```text
Prompt Construction

LLM Invocation

Citation Extraction

Response Generation
```

---

## Gemini Prompt Rules

Allowed Context

```text
Retrieved Chunks
```

Only.

---

Forbidden

```text
General Policy Knowledge

Hallucinated Answers
```

---

## Methods

```java
generateAnswer()

generateCitations()

buildPrompt()
```

---

# 10. Policy Copilot Service

Purpose:

General policy question answering.

Examples:

```text
Is flood damage covered?

Is windshield damage covered?
```

---

## Workflow

```text
Question
      ↓
Vector Search
      ↓
Answer Generation
      ↓
Citation Generation
```

---

## Methods

```java
askQuestion()

getAnswer()

getHistory()
```

---

# 11. Coverage Validation Service

Purpose:

Coverage-related reasoning.

Examples:

```text
Vehicle damaged during flooding

Engine water ingress

Theft claim
```

---

## Methods

```java
validateCoverage()

analyzeCoverageScenario()
```

---

# 12. Investigation Copilot Service

Purpose:

Assists investigators.

Examples:

```text
Show exclusions related to alcohol usage

Show claim conditions
```

---

## Methods

```java
investigatePolicy()

findExclusions()

findRelevantClauses()
```

---

# 13. Chat Session Management

Purpose:

Conversation continuity.

---

## Responsibilities

```text
Create Session

Store Messages

Track History
```

---

## Methods

```java
createSession()

addMessage()

loadConversation()
```

---

# 14. Citation Service

Purpose:

Source traceability.

Every answer must include:

```text
Document Name

Section

Clause Reference
```

---

## Example

```text
Covered under Engine Protection Add-On.

Source:
Policy Wording
Section 5.2
Clause EP-001
```

---

## Methods

```java
buildCitations()

attachSources()
```

---

# 15. AI Audit Service

Purpose:

Complete AI traceability.

Stores:

```text
Question

Retrieved Chunks

Answer

Model

Confidence

Performance Metrics
```

---

## Methods

```java
recordInteraction()

recordRetrieval()

recordAnswer()
```

---

# 16. Worker Design

## Knowledge Build Worker

Purpose:

```text
Process New Policy Documents
```

Workflow:

```text
OCR Complete
      ↓
Chunking
      ↓
Embeddings
      ↓
Knowledge Ready
```

---

## Embedding Regeneration Worker

Purpose:

```text
Model Upgrade Support
```

---

## Rebuild Worker

Purpose:

```text
Knowledge Base Rebuild
```

---

# 17. Gemini Integration

Provider

```text
Gemini
```

---

Capabilities

```text
Answer Generation

Scenario Reasoning

Summarization

Coverage Explanation
```

---

Forbidden Usage

```text
Claim Approval

Claim Rejection

Fraud Decisions
```

---

# 18. Event Publishing

## PolicyIntelligenceEventPublisher

Events

```text
POLICY_CHUNK_CREATED

POLICY_EMBEDDINGS_GENERATED

POLICY_KNOWLEDGE_READY

POLICY_CHAT_SESSION_CREATED

POLICY_QUESTION_ASKED

POLICY_ANSWER_GENERATED

POLICY_CITATION_CREATED

POLICY_KNOWLEDGE_REBUILT
```

---

All Events Use

```text
Outbox Pattern
```

---

# 19. Transaction Boundaries

## Knowledge Ingestion

```java
@Transactional
```

Workflow:

```text
Create Chunks
      ↓
Persist Chunks
      ↓
Save Events
      ↓
Commit
```

---

## Ask Question

```java
@Transactional
```

Workflow:

```text
Store Question
      ↓
Generate Answer
      ↓
Store Answer
      ↓
Store Citations
      ↓
Store Audit Log
      ↓
Commit
```

---

# 20. Exception Handling

Exceptions

```java
KnowledgeBaseNotReadyException

PolicyChunkException

EmbeddingGenerationException

VectorSearchException

AnswerGenerationException

CitationGenerationException

PolicySessionNotFoundException
```

---

# 21. Security Rules

Required Permissions

```text
POLICY_AI_VIEW

POLICY_AI_ASK

POLICY_AI_AUDIT_VIEW

POLICY_AI_ADMIN
```

---

Tenant Isolation

```text
Mandatory
```

---

Product Version Isolation

```text
Mandatory
```

---

# 22. Audit Integration

Tracked Actions

```text
Question Asked

Answer Generated

Knowledge Rebuilt

Embeddings Generated

Coverage Validation

Investigation Query
```

---

Every Significant Action Generates

```text
AUDIT_EVENT_CREATED
```

---

# 23. Performance Targets

Knowledge Build

```text
< 5 Minutes
Per Policy Document
```

---

Vector Search

```text
< 200 ms
```

---

Answer Generation

```text
< 5 Seconds
```

---

Citation Generation

```text
< 100 ms
```

---

# 24. Dependency Diagram

```text
Policy Documents
        ↓
PolicyIngestionService
        ↓
ChunkingService
        ↓
EmbeddingService
        ↓
policy_ai Schema

User Question
        ↓
PolicyCopilotService
        ↓
VectorSearchService
        ↓
AnswerGenerationService
        ↓
Gemini

Answer
        ↓
CitationService
        ↓
AuditService
```

---

# 25. Unit Testing Requirements

Coverage Target

```text
90%+
```

Required Tests

```text
Chunk Creation

Embedding Generation

Vector Search

Answer Generation

Citation Generation

Coverage Validation

Investigation Copilot

Audit Logging
```

---

# 26. Future Enhancements

V2 Reserved

```text
Multi-Policy Reasoning

Cross-Product Analysis

Coverage Graph

Fraud Copilot

Claim Copilot

Clause Comparison

Policy Diff Analysis

Multi-LLM Support
```

---

# Approval

This document defines the Policy Intelligence implementation blueprint and serves as the reference for RAG architecture, vector search, Gemini integration, policy copilots, coverage validation, investigation assistance, citation generation, AI auditability, and future AI expansion.
