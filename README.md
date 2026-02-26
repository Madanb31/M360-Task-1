🚀 Agentic AI User Management Platform
A governed, multi-agent AI system built with Spring Boot and Spring AI, designed to automate user management operations securely. The platform features role-based access control (RBAC), Human-in-the-Loop (HITL) governance, and a deterministic orchestration layer.

🏗️ Architecture
The system follows a Multi-Agent Orchestration pattern where specialized agents handle specific domains (Analysis vs. Management), coordinated by role-specific Orchestrators.

🧠 The Agents
UserAnalysisAgent (The Reader 📖)

Role: Analyzes user profiles, searches database, checks data completeness.
Tools: Deterministic DB lookups (userSearchTool, listAllUsersTool).
Capability: 100% Read-Only. Cannot modify data.
Tech: Uses Chain-of-Thought (CoT) prompting to prevent hallucinations and produce structured JSON reports.
UserManagementAgent (The Writer ✍️)

Role: Handles sensitive operations (Create, Delete, Promote, Demote).
Tools: UserManagementTools (Create/Delete/AssignRole).
Governance: Only accessible via Admin Orchestrator.
The Orchestrators (The Bosses 👔)

ReadOnlyOrchestratorAgent:
For USER role.
Wiring: Only has access to Read Tools.
Security: Impossible to trigger write actions (tools missing).
AdminOrchestratorAgent:
For ADMIN role.
Wiring: Has Read + Write Tools.
Deterministic Routing: Bypasses LLM for exact commands ("delete user", "list admins") to ensure reliability.
🛡️ Security & Governance
1. Human-in-the-Loop (HITL) ✋
Risky actions (Delete User, Assign Admin Role) are not executed immediately by the AI.

Step 1: AI creates an Approval Request (Status: PENDING) in the database.
Step 2: Human Admin reviews the request via UI.
Step 3: On Approval (POST /hitl/actions/{id}/approve), the backend executes the action deterministically.

2. Role-Based Access Control (RBAC) 🔐
JWT Authentication: Stateless security using BCrypt & HS256 tokens.
Endpoint Security:
/ai/orchestrate → Accessible to USER & ADMIN (Read-Only).
/ai/admin/orchestrate → Accessible to ADMIN only (Read/Write).
/hitl/** → Accessible to ADMIN only.

4. Audit Trail 📜
Every AI interaction (Query, Response, Execution Time, Token Usage) is logged to agent_audit_logs.
Every management action (Create/Delete) is logged.

🛠️ Tech Stack
Backend: Java 21, Spring Boot 3.x
AI Framework: Spring AI 1.1.2
LLM: Google Gemini 2.0 Flash (via Google AI Studio)
Database: PostgreSQL
Security: Spring Security, JWT (jjwt)
Frontend: React.js, Bootstrap
Build Tool: Maven

🚀 Key Features Implemented
✅ Function Calling (Tools): AI connects to real PostgreSQL database.
✅ Structured Output: AI returns strict JSON objects (Java Records) instead of random text.
✅ Chat Memory: Context-aware conversations ("Analyze him" refers to previous user).
✅ Hallucination Control: Deterministic routing for critical queries ("List all users" bypasses LLM generation).
✅ Dual-Client Pattern: Uses one ChatClient for tools and another for JSON formatting to ensure reliability.

🏃‍♂️ How to Run
1. Backend
Clone repository.
Set environment variables in application.properties:

  spring.ai.google.genai.api-key=YOUR_GEMINI_KEY
  jwt.secret=YOUR_JWT_SECRET
  
Run mvn spring-boot:run.
Swagger UI: http://localhost:8080/swagger-ui/index.html

2. Frontend
Navigate to frontend/.
Run npm install.
Run npm start.
App available at http://localhost:3000.

🔮 Future Roadmap
->RAG (Retrieval Augmented Generation): Document search using pgvector for policy lookups.
->Advanced Observability: Dashboard for token usage and agent latency.
