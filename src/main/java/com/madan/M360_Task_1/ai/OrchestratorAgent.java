package com.madan.M360_Task_1.ai;

import com.madan.M360_Task_1.ai.tools.OrchestratorTools;
import com.madan.M360_Task_1.ai.tools.UserTools;
import com.madan.M360_Task_1.models.AgentAuditLog;
import com.madan.M360_Task_1.repository.AgentAuditRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrchestratorAgent {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final AgentAuditRepository auditRepository;

    public OrchestratorAgent(ChatClient.Builder builder,
                             OrchestratorTools orchestratorTools,
                             UserTools userTools,
                             ChatMemory chatMemory,
                             AgentAuditRepository auditRepository) {

        this.chatMemory = chatMemory;
        this.auditRepository = auditRepository;

        this.chatClient = builder
                .defaultSystem("""
                        You are the Main Orchestrator Agent.
                        
                        You coordinate tools/agents to fulfill user requests.
                        
                        Available tools:
                        - listAllUsersDetailedTool(): deterministic. Lists ALL users with ID, username, name, email, roles, hasAddress.
                        - listUsersByRoleTool(roleName): deterministic. Lists users filtered by role (ADMIN/USER/etc).
                        - findUsersByNameTool(name): deterministic.
                        - analyzeAllUsersProfilesTool(): deterministic.
                        - analyzeUserProfileTool(userId): deterministic.
                        - analyzeTool(message): free-form analysis tool.
                        - manageTool(message): user management tool.
                        
                        STRICT RULES:
                        1) "find user <name>" or "search user <name>":
                           - MUST call findUsersByNameTool(<name>)
                           - MUST return the tool output (must include ID and Email). No vague confirmations.
                        
                        2) "analyse him/her/them":
                           - MUST extract the most recent userId mentioned in the conversation history.
                           - MUST call analyzeUserProfileTool(that userId)
                           - MUST return the tool output.
                           - Never ask "who is him" if a userId exists in the recent conversation.
                        
                        3) "analyse <name>" or "analyze <name>":
                           - Step A: call findUsersByNameTool(<name>)
                           - If 0 matches: return "No users found"
                           - If 1 match: call analyzeUserProfileTool(that user's ID) and return the tool output
                           - If >1 matches: list all matches and ask the user to specify the exact userId to analyze
                             (do NOT guess which one).
                        
                        - If the user asks for "analysis of all users", "report", "profile completeness check for all users",
                          you MUST call analyzeAllUsersProfilesTool() and return its output.
                          
                        - For "list all users" / "show all users" / "display users":
                          MUST call listAllUsersDetailedTool() and return its output.
                          
                        - For "list all admins" / "show admins" / "display admins":
                          MUST call listUsersByRoleTool("ADMIN") and return its output.
                        
                        4) Never say "I analyzed the user" unless you actually called analyzeUserProfileTool.
                        5) Do not generate fake users or fake IDs. Only use tool outputs.
                        """)
                .defaultTools(orchestratorTools, userTools)
                .build();
    }

    public String orchestrate(String userMessage, String chatId) {
        long startTime = System.currentTimeMillis();

        // 1. Get History (Manual Memory)
        List<Message> history = chatMemory.get(chatId);

        // 2. Call AI with history + new message
        ChatResponse response = chatClient.prompt()
                .messages(history) // Inject history
                .user(userMessage)
                .call()
                .chatResponse();

        long endTime = System.currentTimeMillis();
        String content = response.getResult().getOutput().getText();

        // 3. Save History (Manual Memory)
        chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
        chatMemory.add(chatId, List.of(new AssistantMessage(content)));

        // 4. Audit Log
        int tokens = 0;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            tokens = response.getMetadata().getUsage().getTotalTokens();
        }

        saveLog("OrchestratorAgent", userMessage, content, chatId, endTime - startTime, tokens);

        return content;
    }

    private void saveLog(String agent, String query, String response, String chatId, long time, int tokens) {
        AgentAuditLog log = AgentAuditLog.builder()
                .agentName(agent)
                .userQuery(query)
                .aiResponse(response)
                .chatId(chatId)
                .timestamp(LocalDateTime.now())
                .executionTimeMs(time)
                .totalTokens(tokens)
                .build();
        auditRepository.save(log);
    }
}