<div align="center">

# 🤖 Agentic AI User Management Platform

**A production-grade, governed Multi-Agent AI System** built with Spring Boot & Spring AI.  
Automate user management with security, auditability, and human oversight built in.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring_AI-1.1.2-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![Gemini](https://img.shields.io/badge/Gemini_2.0_Flash-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://ai.google.dev/)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [The Agents](#-the-agents)
- [Security & Governance](#-security--governance)
- [Tech Stack](#-tech-stack)
- [Key Features](#-key-features)
- [Getting Started](#-getting-started)
- [Roadmap](#-roadmap)

---

## 🌟 Overview

This platform implements a **Multi-Agent Orchestration** pattern where specialized AI agents handle distinct domains — coordinated by role-specific orchestrators. Key pillars:

| 🔐 RBAC | ✋ HITL | 📚 RAG | 📜 Audit |
|:---:|:---:|:---:|:---:|
| Role-Based Access Control | Human-in-the-Loop Governance | Chat with Documents | Full Interaction Logging |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Spring Boot API                          │
│          /ai/orchestrate        /ai/admin/orchestrate           │
└──────────────────┬──────────────────────────┬───────────────────┘
                   │                          │
       ┌───────────▼───────────┐  ┌───────────▼──────────────────┐
       │  ReadOnlyOrchestrator │  │      AdminOrchestrator        │
       │    (USER role) 👤     │  │      (ADMIN role) 👑          │
       │   LLM routing only    │  │   LLM  +  Deterministic       │
       └───────────┬───────────┘  └──────┬───────────────────────┘
                   │                     │
       ┌───────────▼───────────┐  ┌──────▼────────────────────────┐
       │   UserAnalysisAgent   │  │     UserManagementAgent        │
       │      📖  Reader        │  │         ✍️  Writer              │
       │                       │  │                                │
       │  • userSearchTool     │  │  • createUserTool              │
       │  • listAllUsersTool   │  │  • deleteUserTool              │
       │  READ-ONLY            │  │  • assignRoleTool              │
       └───────────────────────┘  └──────────────┬────────────────┘
                                                  │
                                      ┌───────────▼────────────┐
                                      │      HITL Gate  ✋       │
                                      │  PENDING → Review →    │
                                      │  Approve / Reject      │
                                      └────────────────────────┘
```

---

## 🧠 The Agents

### `UserAnalysisAgent` — The Reader 📖

| Property | Detail |
|---|---|
| **Role** | Analyzes user profiles, searches the database, checks data completeness |
| **Tools** | `userSearchTool`, `listAllUsersTool` (deterministic DB lookups) |
| **Capability** | **100% Read-Only** — architecturally cannot modify data |
| **Prompting** | Chain-of-Thought (CoT) to prevent hallucinations & produce strict JSON output |

### `UserManagementAgent` — The Writer ✍️

| Property | Detail |
|---|---|
| **Role** | Handles sensitive operations: Create, Delete, Promote, Demote |
| **Tools** | `createUserTool`, `deleteUserTool`, `assignRoleTool` |
| **Access** | **Admin Orchestrator only** — never exposed to USER role |
| **Governance** | Every destructive action gated behind HITL approval |

### The Orchestrators — The Bosses 👔

**`ReadOnlyOrchestratorAgent`** `(USER role)`
- Wired with read tools only — write actions are **architecturally impossible**
- All routing via LLM with CoT reasoning

**`AdminOrchestratorAgent`** `(ADMIN role)`
- Wired with read + write tools
- **Deterministic Routing:** exact commands like `"delete user"` or `"list admins"` bypass the LLM entirely for guaranteed reliability

---

## 🛡️ Security & Governance

### ✋ Human-in-the-Loop (HITL)

Risky operations are **never executed immediately.** Every destructive action goes through a mandatory approval gate.

```
  AI determines action needed
           │
           ▼
  ┌─────────────────────┐
  │  Create Approval    │  ◄── Status: PENDING
  │  Request in DB      │
  └────────┬────────────┘
           │
           ▼
  Admin reviews at /approvals
           │
     ┌─────┴──────┐
     │            │
  [Approve]    [Reject]
     │            │
     ▼            ▼
  Execute      Cancelled
  Action       (no-op)
```

**HITL-gated operations:** 🗑️ Delete User &nbsp;|&nbsp; 🔑 Assign Admin Role

---

### 📚 Retrieval Augmented Generation (RAG)

**Feature:** *Chat with your documents* — upload policy PDFs and query them in natural language.

```
Upload PDF  ──►  Split Chunks  ──►  Generate Embeddings  ──►  Store in pgvector
                                     (Local ONNX Model)
                                                                      │
User Query  ──►  Embed Query   ──────────────────────────►  Retrieve & Answer
```

> **Stack:** pgvector (PostgreSQL) + Spring AI + Apache Tika + Local ONNX Models

---

### 🔐 Role-Based Access Control

**Auth:** Stateless JWT (HS256) with BCrypt password hashing.

| Endpoint | USER | ADMIN |
|---|:---:|:---:|
| `POST /ai/orchestrate` | ✅ | ✅ |
| `POST /ai/admin/orchestrate` | ❌ | ✅ |
| `GET /hitl/**` | ❌ | ✅ |
| `POST /hitl/actions/{id}/approve` | ❌ | ✅ |

---

### 📜 Audit Trail

Every AI interaction is persisted to `agent_audit_logs`:

| Field | Example |
|---|---|
| Query | `"list all users"` |
| Response | `{ "users": [...] }` |
| Execution Time | `142ms` |
| Token Usage | `312 tokens` |

Every management action (Create / Delete) is also independently tracked.

---

### 🗄️ Database Version Control

**Liquibase** manages all schema migrations — tables are created automatically on first run, ensuring production-safe deployments.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 21, Spring Boot 3.5.x |
| **AI Framework** | Spring AI 1.1.2 |
| **LLM** | Google Gemini 2.0 Flash (Google AI Studio) |
| **Embeddings** | Local ONNX Models (`spring-ai-transformers`) |
| **Database** | PostgreSQL + pgvector extension |
| **Migrations** | Liquibase |
| **Security** | Spring Security, JWT (jjwt), BCrypt |
| **Frontend** | React.js, Bootstrap |
| **Build** | Maven |

---

## ✅ Key Features

| Feature | Description |
|---|---|
| 🔧 **Function Calling** | AI connects to real PostgreSQL data via Spring AI tools |
| 📊 **Structured Output** | Returns strict Java Records (JSON) — no free-form text |
| 🧠 **Chat Memory** | Context-aware sessions — *"Analyze him"* resolves to previous subject |
| 🎯 **Hallucination Control** | Deterministic routing for critical queries bypasses LLM generation |
| 🔀 **Dual-Client Pattern** | Separate `ChatClient` for tool execution vs. JSON formatting |
| 📚 **RAG** | Upload & chat with PDFs — policy documents, employee handbooks, etc. |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven
- PostgreSQL with `pgvector` extension
- Google AI Studio API key

### 1. Start PostgreSQL with pgvector

```bash
docker run -d \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=yourpassword \
  -e POSTGRES_DB=agenticdb \
  ankane/pgvector
```

### 2. Configure the Backend

```properties
# application.properties
spring.ai.google.genai.api-key=YOUR_GEMINI_KEY
jwt.secret=YOUR_JWT_SECRET
spring.datasource.url=jdbc:postgresql://localhost:5432/agenticdb
spring.datasource.password=yourpassword
```

### 3. Run the Backend

```bash
git clone https://github.com/your-username/agentic-user-management.git
cd agentic-user-management
mvn spring-boot:run
```

> ✅ Liquibase runs automatically and creates all tables.  
> 🔑 Default admin created: `admin` / `admin123`  
> 📖 Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### 4. Run the Frontend

```bash
cd frontend
npm install
npm start
```

> 🌐 App available at [http://localhost:3000](http://localhost:3000)

---

## 🔮 Roadmap

- [ ] **Advanced Observability** — Dashboard for token usage & agent latency metrics
- [ ] **Multi-Modal AI** — Support image inputs for user profile analysis
- [ ] **Email Agents** — AI agent sends notifications on user creation / deletion
- [ ] **Multi-tenant Support** — Isolated agent contexts per organization

---

<div align="center">

Built with ☕ Java &nbsp;|&nbsp; 🤖 Spring AI &nbsp;|&nbsp; 🛡️ Responsible AI Governance

*Contributions, issues, and feature requests are welcome!*

</div>
