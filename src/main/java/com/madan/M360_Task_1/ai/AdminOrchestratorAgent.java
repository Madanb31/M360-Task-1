package com.madan.M360_Task_1.ai;

import com.madan.M360_Task_1.ai.tools.RagTools;
import com.madan.M360_Task_1.ai.tools.ReadOnlyOrchestratorTools;
import com.madan.M360_Task_1.ai.tools.UserCreateTools;
import com.madan.M360_Task_1.ai.tools.UserTools;
import com.madan.M360_Task_1.hitl.ActionRequestService;
import com.madan.M360_Task_1.models.ActionRequest;
import com.madan.M360_Task_1.models.AgentAuditLog;
import com.madan.M360_Task_1.repository.AgentAuditRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AdminOrchestratorAgent {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final AgentAuditRepository auditRepository;
    private final UserTools userTools;
    private final ActionRequestService actionRequestService;

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public AdminOrchestratorAgent(ChatClient.Builder builder,
            ReadOnlyOrchestratorTools readOnlyTools,
            UserTools userTools,
            UserCreateTools userCreateTools,
            RagTools ragTools,
            ChatMemory chatMemory,
            AgentAuditRepository auditRepository,
            ActionRequestService actionRequestService) {

        this.chatMemory = chatMemory;
        this.auditRepository = auditRepository;
        this.actionRequestService = actionRequestService;
        this.userTools = userTools;

        this.chatClient = builder
                .defaultSystem(
                        """
                                You are the ADMIN Orchestrator Agent.

                                Governance:
                                - For read tasks, use deterministic tools (UserTools) or analyzeTool.
                                - For creating users, use createUserNowTool.
                                - For delete/role changes, the backend will create approval requests (HITL).
                                - Never invent IDs.
                                """)
                // NOTE: No HITL tool needed now; we create requests deterministically in Java.
                .defaultTools(readOnlyTools, userTools, userCreateTools, ragTools)
                .build();
    }

    public String orchestrate(String userMessage, String chatId) {
        return orchestrateWithThinking(userMessage, chatId).answer();
    }

    public ThinkingResponse orchestrateWithThinking(String userMessage, String chatId) {
        long startTime = System.currentTimeMillis();

        List<Message> history = chatMemory.get(chatId);

        String deterministic = handleDeterministicRead(userMessage);
        if (deterministic != null) {
            chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
            chatMemory.add(chatId, List.of(new AssistantMessage(deterministic)));
            saveLog("AdminOrchestratorAgent", userMessage, deterministic, chatId,
                    System.currentTimeMillis() - startTime, 0);
            return new ThinkingResponse(deterministic, null);
        }

        // 1) Deterministic HITL routing for risky actions
        String hitlResponse = tryHandleHitlDeterministically(userMessage, history);
        if (hitlResponse != null) {
            // save memory
            chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
            chatMemory.add(chatId, List.of(new AssistantMessage(hitlResponse)));

            // audit log
            saveLog("AdminOrchestratorAgent", userMessage, hitlResponse, chatId,
                    System.currentTimeMillis() - startTime, 0);

            return new ThinkingResponse(hitlResponse, null);
        }

        // 2) Normal LLM orchestration for non-risky actions
        ChatResponse response = chatClient.prompt()
                .messages(history)
                .user(userMessage)
                .call()
                .chatResponse();

        AssistantMessage assistantMessage = response.getResult().getOutput();
        String content = assistantMessage.getText();

        String thinking = "Searching company knowledge base and documents to find relevant information for: \"" + userMessage + "\"";

        chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
        chatMemory.add(chatId, List.of(new AssistantMessage(content)));

        int tokens = 0;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            tokens = response.getMetadata().getUsage().getTotalTokens();
        }

        saveLog("AdminOrchestratorAgent", userMessage, content, chatId,
                System.currentTimeMillis() - startTime, tokens);

        return new ThinkingResponse(content, thinking);
    }

    private String handleDeterministicRead(String msg) {
        if (msg == null)
            return null;

        String m = msg.trim().toLowerCase();

        // List all users
        if (m.equals("list all users")
                || m.equals("list users")
                || m.equals("show all users")
                || m.equals("show users")
                || m.equals("display all users")
                || m.equals("display users")) {
            return userTools.listAllUsersDetailedTool();
        }

        // List all admins
        if (m.equals("list all admins")
                || m.equals("list admins")
                || m.equals("show admins")
                || m.equals("display admins")) {
            return userTools.listUsersByRoleTool("ADMIN");
        }

        // Profile completeness report for all users
        if ((m.contains("profile completeness") && m.contains("all"))
                || (m.contains("analysis report") && m.contains("all"))
                || m.contains("analyze all users")
                || m.contains("analyse all users")
                || m.contains("completeness check for all users")) {
            return userTools.analyzeAllUsersProfilesTool();
        }

        // Find/Search user <name>
        if (m.startsWith("find user ")) {
            String name = msg.substring("find user ".length()).trim();
            if (!name.isBlank())
                return userTools.findUsersByNameTool(name);
        }

        if (m.startsWith("search user ")) {
            String name = msg.substring("search user ".length()).trim();
            if (!name.isBlank())
                return userTools.findUsersByNameTool(name);
        }

        return null;
    }

    private String tryHandleHitlDeterministically(String userMessage, List<Message> history) {
        String msg = userMessage.toLowerCase();

        boolean isDelete = msg.contains("delete");
        boolean isAssignAdmin = msg.contains("assign admin")
                || msg.contains("make him admin")
                || msg.contains("make her admin")
                || msg.contains("make admin")
                || msg.contains("promote");
        boolean isRemoveAdmin = msg.contains("remove admin")
                || msg.contains("demote")
                || msg.contains("remove role admin");

        // Only handle these risky actions here
        if (!isDelete && !isAssignAdmin && !isRemoveAdmin) {
            return null;
        }

        // Extract userId from message or history
        String userId = extractUuid(userMessage);
        if (userId == null) {
            userId = extractLastUuidFromHistory(history);
        }

        if (userId == null) {
            return "❌ Approval request not created. Please provide the user ID (UUID) or search the user first so I can pick the last user ID from context.";
        }

        String requestedBy = currentUsername();

        ActionRequest req;
        if (isDelete) {
            req = actionRequestService.requestDeleteUser(userId, requestedBy);
            return approvalCard(req.getId().toString(), "DELETE_USER", userId, "N/A", requestedBy);
        }

        if (isAssignAdmin) {
            req = actionRequestService.requestAssignRole(userId, "ADMIN", requestedBy);
            return approvalCard(req.getId().toString(), "ASSIGN_ROLE", userId, "ADMIN", requestedBy);
        }

        // remove admin
        req = actionRequestService.requestRemoveRole(userId, "ADMIN", requestedBy);
        return approvalCard(req.getId().toString(), "REMOVE_ROLE", userId, "ADMIN", requestedBy);
    }

    private String extractUuid(String text) {
        Matcher m = UUID_PATTERN.matcher(text);
        return m.find() ? m.group() : null;
    }

    private String extractLastUuidFromHistory(List<Message> history) {
        // search last UUID in all message contents from latest to oldest
        for (int i = history.size() - 1; i >= 0; i--) {
            String text = history.get(i).getText();
            String uuid = extractUuid(text);
            if (uuid != null)
                return uuid;
        }
        return null;
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "unknown";
    }

    private String approvalCard(String requestId, String action, String userId, String role, String requestedBy) {
        return """
                🛑 Approval Required (PENDING)

                RequestId: %s
                Action: %s
                TargetUserId: %s
                Role: %s
                RequestedBy: %s

                To Approve:
                POST /hitl/actions/%s/approve

                To Reject:
                POST /hitl/actions/%s/reject
                """.formatted(requestId, action, userId, role, requestedBy, requestId, requestId);
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