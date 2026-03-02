# 🤖 Agentic AI User Management Platform

> A governed, multi-agent AI system built with **Spring Boot** and **Spring AI** — automating user management operations with security, auditability, and human oversight at its core.

---

## 📐 Architecture Overview

The platform follows a **Multi-Agent Orchestration** pattern where specialized agents handle distinct domains, coordinated by role-specific orchestrators.

```
                        ┌──────────────────────────────────────┐
                        │           Spring Boot API             │
                        │  /ai/orchestrate  /ai/admin/orchestrate│
                        └──────────┬──────────────┬────────────┘
                                   │              │
               ┌───────────────────▼──┐    ┌──────▼───────────────────┐
               │  ReadOnlyOrchestrator │    │   AdminOrchestrator       │
               │     (USER role)       │    │   (ADMIN role)            │
               │  ─ LLM routing only ─ │    │  ─ LLM + Deterministic ─ │
               └───────────┬──────────┘    └──────┬────────────────────┘
                           │                      │
               ┌───────────▼──────────┐  ┌────────▼───────────────────┐
               │   UserAnalysisAgent  │  │    UserManagementAgent      │
               │      📖 Reader        │  │       ✍️  Writer             │
               │  userSearchTool      │  │  createUserTool             │
               │  listAllUsersTool    │  │  deleteUserTool             │
               │  READ-ONLY           │  │  assignRoleTool             │
               └──────────────────────┘  └─────────────┬──────────────┘
                                                        │
                                            ┌───────────▼────────────┐
                                            │     HITL Gate ✋         │
                                            │  Approval Required for  │
                                            │  Delete / Promote ops   │
                                            └────────────────────────┘
```

---

## 🧠 The Agents

### `UserAnalysisAgent` — The Reader 📖

| Property | Detail |
|---|---|
| **Role** | Analyzes user profiles, searches the database, checks data completeness |
| **Tools** | `userSearchTool`, `listAllUsersTool` (deterministic DB lookups) |
| **Capability** | **100% Read-Only** — cannot modify any data |
| **Prompting** | Chain-of-Thought (CoT) to prevent hallucinations & produce structured JSON |

### `UserManagementAgent` — The Writer ✍️

| Property | Detail |
|---|---|
| **Role** | Handles sensitive operations: Create, Delete, Promote, Demote |
| **Tools** | `createUserTool`, `deleteUserTool`, `assignRoleTool` |
| **Access** | **Admin Orchestrator only** — never exposed to USER role |

---

## 👔 The Orchestrators

### `ReadOnlyOrchestratorAgent` (USER role)
- Wired with **read tools only** — write actions are architecturally impossible
- All requests routed through LLM with CoT reasoning

### `AdminOrchestratorAgent` (ADMIN role)
- Wired with **read + write tools**
- **Deterministic Routing**: exact commands like `"delete user"` or `"list admins"` bypass the LLM entirely for maximum reliability

---

## 🛡️ Security & Governance

### ✋ Human-in-the-Loop (HITL)

Risky operations are **never executed immediately** by the AI.

```
AI decides action needed
        │
        ▼
Create Approval Request (Status: PENDING)
        │
        ▼
Admin reviews via UI  ──[Reject]──► Action Cancelled
        │
     [Approve]
        │
        ▼
Backend executes deterministically
```

**HITL-gated operations:**
- 🗑️ Delete User
- 🔑 Assign Admin Role

---

### 🔐 Role-Based Access Control (RBAC)

| Endpoint | USER | ADMIN |
|---|:---:|:---:|
| `POST /ai/orchestrate` | ✅ | ✅ |
| `POST /ai/admin/orchestrate` | ❌ | ✅ |
| `GET/POST /hitl/**` | ❌ | ✅ |

**Auth:** Stateless JWT (HS256) with BCrypt password hashing.

---

### 📜 Audit Trail

Every AI interaction is logged to `agent_audit_logs`:

```
┌─────────────────┬──────────────┬──────────────┬─────────────┐
│   Query         │  Response    │  Exec Time   │ Token Usage │
├─────────────────┼──────────────┼──────────────┼─────────────┤
│ "list all users"│  {...}       │  142ms       │  312 tokens │
│ "delete alice"  │  HITL_PEND  │  98ms        │  201 tokens │
└─────────────────┴──────────────┴──────────────┴─────────────┘
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 21, Spring Boot 3.x |
| **AI Framework** | Spring AI 1.1.2 |
| **LLM** | Google Gemini 2.0 Flash (Google AI Studio) |
| **Database** | PostgreSQL |
| **Security** | Spring Security, JWT (jjwt), BCrypt |
| **Frontend** | React.js, Bootstrap |
| **Build** | Maven |

---

## ✅ Key Features

| Feature | Description |
|---|---|
| 🔧 **Function Calling** | AI connects to real PostgreSQL database via Spring AI tools |
| 📊 **Structured Output** | AI returns strict Java Records (JSON) — no free-form text |
| 🧠 **Chat Memory** | Context-aware sessions — *"Analyze him"* resolves to previous subject |
| 🎯 **Hallucination Control** | Deterministic routing for critical queries bypasses LLM generation |
| 🔀 **Dual-Client Pattern** | Separate `ChatClient` for tool execution vs. JSON formatting |

---

## 🚀 Getting Started

### Backend

```bash
# 1. Clone the repository
git clone https://github.com/your-username/agentic-user-management.git
cd agentic-user-management

# 2. Set environment variables in application.properties
spring.ai.google.genai.api-key=YOUR_GEMINI_KEY
jwt.secret=YOUR_JWT_SECRET

# 3. Run
mvn spring-boot:run
```

📖 **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### Frontend

```bash
cd frontend
npm install
npm start
```

🌐 **App:** [http://localhost:3000](http://localhost:3000)

---

## 🔮 Roadmap

- [ ] **RAG Integration** — Document search with `pgvector` for policy lookups
- [ ] **Observability Dashboard** — Real-time token usage & agent latency metrics
- [ ] **Multi-tenant Support** — Isolated agent contexts per organization
- [ ] **Webhook Notifications** — HITL approval requests via Slack / email

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to open an issue or submit a pull request.

---

<p align="center">Built with ☕ Java, 🤖 Spring AI, and a commitment to <strong>responsible AI governance</strong></p>
