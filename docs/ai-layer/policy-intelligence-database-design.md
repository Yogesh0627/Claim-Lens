> **⚠️ Design-era document — reconciled against the as-built system on 2026-07-29.** Written before implementation; where it diverges from the shipped code the authoritative sources win: [`../domain-model.md`](../domain-model.md), [`../architecture.md`](../architecture.md), [`../audit-report.md`](../audit-report.md), and the running API. Deltas flagged inline as **As-built** notes.

# 15.2 Database Design Part 15 – Policy Intelligence Domain

Status: Approved

Version: 1.0

Owner: Niyo Technologies

---

# Purpose

This domain manages:

* Policy Knowledge Storage
* RAG Retrieval Data
* Embeddings
* AI Conversations
* Question History
* AI Auditability

This domain serves as the foundation for:

* Policy Copilot
* Coverage Validation
* Scenario Analysis
* Investigation Copilot
* AI-Powered Policy Research

---

# Schema Strategy

Dedicated Schema:

```sql
policy_ai
```

**As-built (2026-07-29):** The RAG tables shipped in migration **V22 (`policy_ai_tables`)** as part of the `coverage` module. Only **three** tables/entities were built — `policy_chunk` (`PolicyChunk`), plus `coverage_answer` (`CoverageAnswer`) and `coverage_citation` (`CoverageCitation`). The `policy_embedding`, `policy_chat_session`, `policy_chat_message`, `policy_question`, `policy_answer`, `policy_answer_citation`, and `coverage_question_log` tables below were **not built**. Embeddings are not a separate table: when `PGVECTOR_ENABLED=true` a pgvector column with an HNSW index is used; otherwise chunk text is stored and cosine similarity is computed in Java. Offline stub embeddings are 256-dim hashes (not `VECTOR(768)`).

Purpose:

```text
Logical Separation

AI Data Isolation

Independent Scaling

Future Service Extraction
```

---

# RAG Architecture

```text
Product Documents
        ↓
OCR
        ↓
Chunking
        ↓
policy_chunk
        ↓
Embeddings
        ↓
policy_embedding
        ↓
Vector Search
        ↓
Gemini
        ↓
Answer
```

---

# PostgreSQL Extension

Required Extension

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

---

# Table: policy_chunk

Purpose:

Stores searchable policy text chunks.

Schema:

policy_ai

---

Columns

| Column              | Type         | Constraints |
| ------------------- | ------------ | ----------- |
| id                  | BIGSERIAL    | PRIMARY KEY |
| tenant_id           | BIGINT       | NOT NULL    |
| product_version_id  | BIGINT       | NOT NULL    |
| product_document_id | BIGINT       | NOT NULL    |
| chunk_number        | INTEGER      | NOT NULL    |
| section_name        | VARCHAR(500) |             |
| clause_reference    | VARCHAR(255) |             |
| chunk_text          | TEXT         | NOT NULL    |
| token_count         | INTEGER      |             |
| created_at          | TIMESTAMP    | NOT NULL    |

---

Foreign Keys

```sql
product_version_id
    REFERENCES insurance_product_version(id)

product_document_id
    REFERENCES product_document(id)
```

---

Example

```text
Section 5.2

Engine Protection Add-On

Water ingress damage is covered...
```

---

Indexes

```sql
CREATE INDEX idx_policy_chunk_version
ON policy_ai.policy_chunk(product_version_id);

CREATE INDEX idx_policy_chunk_document
ON policy_ai.policy_chunk(product_document_id);
```

---

# Table: policy_embedding

**As-built (2026-07-29):** No standalone `policy_embedding` table. See the Schema Strategy note — embeddings live in a pgvector column (HNSW index) when enabled, else similarity is computed in-Java over stored chunk text.

Purpose:

Stores vector embeddings used for semantic search.

Schema:

policy_ai

---

Columns

| Column           | Type         |
| ---------------- | ------------ |
| id               | BIGSERIAL    |
| chunk_id         | BIGINT       |
| embedding_model  | VARCHAR(100) |
| embedding_vector | VECTOR(768)  |
| created_at       | TIMESTAMP    |

---

Foreign Keys

```sql
chunk_id
    REFERENCES policy_ai.policy_chunk(id)
```

---

Embedding Models

Examples:

```text
text-embedding-004

gemini-embedding
```

---

Vector Index

```sql
CREATE INDEX idx_policy_embedding_vector
ON policy_ai.policy_embedding
USING ivfflat (embedding_vector vector_cosine_ops);
```

**As-built (2026-07-29):** The shipped vector index is **HNSW** (pgvector) on the chunk embedding, gated by `PGVECTOR_ENABLED`, not `ivfflat` on a separate embedding table.

---

Purpose

Supports:

```text
Nearest Neighbor Search

Semantic Search

RAG Retrieval
```

---

# Table: policy_chat_session

**As-built (2026-07-29):** Not built — coverage Q&A is stateless. The `policy_chat_session`, `policy_chat_message`, `policy_question`, `policy_answer`, `policy_answer_citation`, and `coverage_question_log` tables that follow do not exist. Answers and their sources are persisted as `coverage_answer` + `coverage_citation` instead.

Purpose:

Represents AI conversations.

Schema:

policy_ai

---

Columns

| Column             | Type         |
| ------------------ | ------------ |
| id                 | BIGSERIAL    |
| tenant_id          | BIGINT       |
| user_id            | BIGINT       |
| product_version_id | BIGINT       |
| session_name       | VARCHAR(255) |
| created_at         | TIMESTAMP    |
| last_activity_at   | TIMESTAMP    |

---

Foreign Keys

```sql
user_id
    REFERENCES user(id)

product_version_id
    REFERENCES insurance_product_version(id)
```

---

Indexes

```sql
CREATE INDEX idx_chat_session_user
ON policy_ai.policy_chat_session(user_id);

CREATE INDEX idx_chat_session_product
ON policy_ai.policy_chat_session(product_version_id);
```

---

# Table: policy_chat_message

Purpose:

Stores chat messages.

Schema:

policy_ai

---

Columns

| Column       | Type        |
| ------------ | ----------- |
| id           | BIGSERIAL   |
| session_id   | BIGINT      |
| role         | VARCHAR(50) |
| message_text | TEXT        |
| created_at   | TIMESTAMP   |

---

Roles

```text
USER

ASSISTANT
```

---

Foreign Keys

```sql
session_id
    REFERENCES policy_ai.policy_chat_session(id)
```

---

# Table: policy_question

Purpose:

Stores user questions.

Schema:

policy_ai

---

Columns

| Column        | Type      |
| ------------- | --------- |
| id            | BIGSERIAL |
| session_id    | BIGINT    |
| question_text | TEXT      |
| created_by    | BIGINT    |
| created_at    | TIMESTAMP |

---

Foreign Keys

```sql
session_id
    REFERENCES policy_ai.policy_chat_session(id)
```

---

# Table: policy_answer

Purpose:

Stores generated answers.

Schema:

policy_ai

---

Columns

| Column            | Type         |
| ----------------- | ------------ |
| id                | BIGSERIAL    |
| question_id       | BIGINT       |
| answer_text       | TEXT         |
| model_name        | VARCHAR(100) |
| confidence_score  | NUMERIC(5,2) |
| prompt_tokens     | INTEGER      |
| completion_tokens | INTEGER      |
| created_at        | TIMESTAMP    |

---

Foreign Keys

```sql
question_id
    REFERENCES policy_ai.policy_question(id)
```

---

# Table: policy_answer_citation

Purpose:

Stores answer citations.

Schema:

policy_ai

---

Columns

| Column          | Type         |
| --------------- | ------------ |
| id              | BIGSERIAL    |
| answer_id       | BIGINT       |
| chunk_id        | BIGINT       |
| relevance_score | NUMERIC(5,2) |

---

Foreign Keys

```sql
answer_id
    REFERENCES policy_ai.policy_answer(id)

chunk_id
    REFERENCES policy_ai.policy_chunk(id)
```

---

Purpose

Provides:

```text
Source Traceability

Clause References

Answer Verification
```

---

# Table: coverage_question_log

Purpose:

Provides AI auditability.

Schema:

policy_ai

---

Columns

| Column             | Type         |
| ------------------ | ------------ |
| id                 | BIGSERIAL    |
| tenant_id          | BIGINT       |
| user_id            | BIGINT       |
| session_id         | BIGINT       |
| question_id        | BIGINT       |
| answer_id          | BIGINT       |
| model_name         | VARCHAR(100) |
| retrieval_time_ms  | INTEGER      |
| generation_time_ms | INTEGER      |
| confidence_score   | NUMERIC(5,2) |
| created_at         | TIMESTAMP    |

---

Purpose

Stores:

```text
Question

Answer

Model

Performance

Confidence

Audit Trail
```

---

# Chunking Strategy

Target Chunk Size

```text
500-1000 Tokens
```

---

Overlap

```text
100-150 Tokens
```

---

Reason

```text
Better Retrieval

Improved Context

More Accurate Answers
```

---

# Retrieval Strategy

Workflow

```text
User Question
        ↓
Embedding Generation
        ↓
Vector Search
        ↓
Top 10 Chunks
        ↓
Context Assembly
        ↓
Gemini
        ↓
Answer
```

---

# AI Grounding Rule

Gemini receives:

```text
Retrieved Chunks Only
```

Never:

```text
Entire Document
```

---

# Product Version Rule

Every retrieval must filter by:

```sql
product_version_id
```

Purpose:

```text
Version Accurate Answers
```

Example:

```text
Private Car Premium V1
```

must never retrieve:

```text
Private Car Premium V2
```

knowledge.

---

# Data Retention

Policy Chunks

```text
Never Hard Delete
```

---

Embeddings

```text
Regenerated When Needed
```

---

Questions

```text
Retain 7 Years
```

---

Answers

```text
Retain 7 Years
```

---

Audit Logs

```text
Retain 7 Years
```

---

# Performance Targets

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

Chat History Retrieval

```text
< 100 ms
```

---

# Security Rules

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

Question Auditability

```text
Mandatory
```

---

Citation Support

```text
Mandatory
```

---

# Domain Summary

Tables

```text
policy_chunk

policy_embedding

policy_chat_session

policy_chat_message

policy_question

policy_answer

policy_answer_citation

coverage_question_log
```

**As-built (2026-07-29):** Actual tables (V22): `policy_chunk`, `coverage_answer`, `coverage_citation` only.

Vector Database

```text
PostgreSQL + pgvector
```

LLM

```text
Gemini
```

Citation Support

```text
YES
```

Version-Aware Retrieval

```text
YES
```

AI Auditability

```text
YES
```

Enterprise RAG Architecture

```text
YES
```
