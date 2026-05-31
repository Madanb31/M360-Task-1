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
    private final RagTools ragTools;
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
        this.ragTools = ragTools;

        this.chatClient = builder
                .defaultSystem(
                        """
                                You are the ADMIN Orchestrator Agent.

                                Governance:
                                - For read tasks, use deterministic tools (UserTools) or analyzeTool.
                                - For creating users, use createUserNowTool.
                                - For delete/role changes, the backend will create approval requests (HITL).
                                - Never invent IDs.

                                Proactive Navigation:
                                When you answer a question that is clearly related to a specific page in the application,
                                proactively suggest navigating there by appending a goToPage tool call JSON at the very
                                end of your response — AFTER your full answer, not before.
                                The user will be shown a confirmation card to accept or reject the navigation.

                                Page suggestions based on context:
                                - If answer is about pending approvals, approval requests → suggest goToPage approvals
                                - If answer is about users, user list, user details, user management → suggest goToPage users
                                - If answer is about roles, permissions, role management → suggest goToPage roles
                                - If answer is about knowledge base, documents, policies, handbook → suggest goToPage knowledge
                                - If answer is about AI chat, assistant → suggest goToPage chat
                                - If answer is about dashboard, overview, summary → suggest goToPage dashboard

                                Format for proactive navigation (JSON must be at the very END of your response):
                                [your full answer here]
                                {"toolCall": "goToPage", "arguments": {"pageId": "approvals"}}

                                Important rules:
                                - Only suggest navigation when it is genuinely helpful and relevant
                                - Do NOT suggest navigation for every response — only when the context clearly maps to a page
                                - Do NOT suggest navigation if the user is already on that page
                                - The JSON must be the very last thing in your response
                                - Only ONE navigation suggestion per response
                                - Do not suggest navigation for general questions like "what is the refund policy"
                                """)
                // NOTE: No HITL tool needed now; we create requests deterministically in Java.
                .defaultTools(readOnlyTools, userTools, userCreateTools, ragTools)
                .build();
    }

    public String orchestrate(String userMessage, String chatId) {
        return orchestrateWithThinking(userMessage, chatId, null, null, true).answer();
    }

    public ThinkingResponse orchestrateWithThinking(String userMessage, String chatId,
            List<AgUiParameters.FrontendToolDefinition> frontendTools, AgUiParameters.UiState uiState, boolean saveToMemory) {
        long startTime = System.currentTimeMillis();

        List<Message> history = chatMemory.get(chatId);

        String deterministic = handleDeterministicRead(userMessage);
        if (deterministic != null) {
            if (saveToMemory) {
                chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
                chatMemory.add(chatId, List.of(new AssistantMessage(deterministic)));
            }
            saveLog("AdminOrchestratorAgent", userMessage, deterministic, chatId,
                    System.currentTimeMillis() - startTime, 0);
            return new ThinkingResponse(deterministic, null);
        }

        // 1) Deterministic HITL routing for risky actions
        String hitlResponse = tryHandleHitlDeterministically(userMessage, history);
        boolean mightHaveUiAction = false;

        if (hitlResponse != null) {
            // Don't return early if there are frontend tools in the message
            // Check if message might also contain a UI action
            mightHaveUiAction = frontendTools != null && !frontendTools.isEmpty()
                    && frontendTools.stream()
                            .anyMatch(tool -> userMessage.toLowerCase().contains(tool.getName().toLowerCase()) ||
                                    isUiActionKeyword(userMessage));

            if (!mightHaveUiAction) {
                // No UI action — return early as before
                if (saveToMemory) {
                    chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
                    chatMemory.add(chatId, List.of(new AssistantMessage(hitlResponse)));
                }
                saveLog("AdminOrchestratorAgent", userMessage, hitlResponse, chatId,
                        System.currentTimeMillis() - startTime, 0);
                return new ThinkingResponse(hitlResponse, null);
            }

            // Has potential UI action — inject HITL result and continue to LLM
            // We will inject it into fullUserMessage below
            if (saveToMemory) {
                chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
            }
            // Continue to LLM call below — don't return
        }

        // 2) Normal LLM orchestration for non-risky actions
        String fullUserMessage = userMessage;

        if (hitlResponse != null && mightHaveUiAction) {
            fullUserMessage = fullUserMessage + "\n\n" +
                    "SYSTEM NOTE: The following approval request has already been created " +
                    "for the risky action in this message. Include this approval card in your response " +
                    "and also handle any other parts of the request:\n" + hitlResponse;
        }
        try {
            if (frontendTools != null && !frontendTools.isEmpty()) {
                String frontendToolsPrompt = """

                        IMPORTANT - UI CONTROL TOOLS:
                        You have access to these UI tools that run in the browser:
                        %s

                        STRICT RULES for using these UI tools:
                        1. NEVER use native function calling for these tools
                        2. ALWAYS output them as plain JSON text in this exact format:
                           {"toolCall": "toolName", "arguments": {"param": "value"}}
                        3. The JSON must be the very first thing in your response if calling a UI tool
                        4. Only ONE UI tool call per response
                        5. After the JSON, you MUST continue with a normal text response to answer
                           any other part of the user's question — NEVER stop after the JSON alone
                        6. If the user asks to do a UI action AND asks a question or requests data,
                           output the JSON first on line 1, then answer the question fully on the next lines
                        7. Example format:
                           {"toolCall": "toggleUiMode", "arguments": {"mode": "dark"}}
                           Here are all the users: [list of users...]
                        """
                        .formatted(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(frontendTools));
                fullUserMessage = fullUserMessage + "\n\n" + frontendToolsPrompt;
            }
        } catch (Exception e) {
        }

        // Inject UI state context if available
        if (uiState != null) {
            String uiContext = String.format("""

                CURRENT UI CONTEXT (use this to make smarter decisions):
                - Current page: %s
                - Current theme: %s
                - Logged in user: %s (%s)

                Rules based on UI context:
                - If user asks to switch to dark mode but theme is already "dark" — tell them it's already dark
                - If user asks to go to a page but currentPage already matches — tell them they're already there
                - If user asks to switch to light mode but theme is already "light" — tell them it's already light
                - Use the current page context to give more relevant answers and suggestions
                """,
                uiState.getCurrentPage() != null ? uiState.getCurrentPage() : "unknown",
                uiState.getTheme() != null ? uiState.getTheme() : "unknown",
                uiState.getUsername() != null ? uiState.getUsername() : "unknown",
                uiState.getUserRole() != null ? uiState.getUserRole() : "unknown"
            );
            fullUserMessage = fullUserMessage + uiContext;
        }

        // After deterministic check but before LLM call
        // Check if message contains a data keyword alongside UI action
        String preFetchedData = null;
        String msgLower = userMessage.toLowerCase();

        if (msgLower.contains("find all users") || msgLower.contains("list all users")
                || msgLower.contains("show all users") || msgLower.contains("list users")
                || msgLower.contains("show users") || msgLower.contains("find users")) {
            preFetchedData = userTools.listAllUsersDetailedTool();
        } else if (msgLower.contains("list all admins") || msgLower.contains("show admins")
                || msgLower.contains("list admins")) {
            preFetchedData = userTools.listUsersByRoleTool("ADMIN");
        }

        // NEW — pre-fetch RAG data for common policy keywords
        // Only do this when message also contains a UI action
        boolean hasUiAction = msgLower.contains("switch") || msgLower.contains("toggle")
                || msgLower.contains("dark mode") || msgLower.contains("light mode")
                || msgLower.contains("go to") || msgLower.contains("navigate")
                || msgLower.contains("background");

        boolean hasRagQuery = msgLower.contains("policy") || msgLower.contains("benefit")
                || msgLower.contains("leave") || msgLower.contains("refund")
                || msgLower.contains("holiday") || msgLower.contains("vacation");

        if (hasUiAction && hasRagQuery && preFetchedData == null) {
            // Extract the question part and pre-fetch via RAG
            preFetchedData = ragTools.searchPolicyTool(userMessage);
        }

        // If data was pre-fetched, inject it into the message
        if (preFetchedData != null) {
            fullUserMessage = fullUserMessage + "\n\nHere is the data to include in your response:\n" + preFetchedData;
        }

        ChatResponse response = chatClient.prompt()
                .messages(history)
                .user(fullUserMessage)
                .call()
                .chatResponse();

        AssistantMessage assistantMessage = response.getResult().getOutput();
        String content = assistantMessage.getText();

        String thinking = "Searching company knowledge base and documents to find relevant information for: \""
                + userMessage + "\"";

        boolean isFrontendToolCall = content != null && content.trim().contains("toolCall");
        boolean hitlAlreadySavedUserMessage = hitlResponse != null && mightHaveUiAction;

        if (saveToMemory) {
            if (!hitlAlreadySavedUserMessage) {
                chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
            }
            if (!isFrontendToolCall) {
                chatMemory.add(chatId, List.of(new AssistantMessage(content)));
            } else {
                // Extract text part — could be before or after JSON
                int jsonStart = content.indexOf('{');
                int jsonEnd = content.lastIndexOf('}');
                String beforeJson = jsonStart > 0 ? content.substring(0, jsonStart).trim() : "";
                String afterJson = jsonEnd >= 0 && jsonEnd < content.length() - 1
                        ? content.substring(jsonEnd + 1).trim()
                        : "";
                String textToSave = beforeJson.isEmpty() ? afterJson : beforeJson;
                if (!textToSave.isEmpty()) {
                    chatMemory.add(chatId, List.of(new AssistantMessage(textToSave)));
                }
            }
        }

        int tokens = 0;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            tokens = response.getMetadata().getUsage().getTotalTokens();
        }

        saveLog("AdminOrchestratorAgent", userMessage, content, chatId,
                System.currentTimeMillis() - startTime, tokens);

        return new ThinkingResponse(content, thinking);
    }

    private boolean isUiActionKeyword(String message) {
        String m = message.toLowerCase();
        return m.contains("switch to") || m.contains("toggle")
                || m.contains("go to") || m.contains("navigate to")
                || m.contains("open page") || m.contains("background")
                || m.contains("dark mode") || m.contains("light mode")
                || m.contains("theme") || m.contains("colour")
                || m.contains("color");
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
                || msg.contains("as admin")
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
            // Try to extract a name from the message and look up user by name
            String name = extractNameFromHitlMessage(userMessage);
            if (name != null) {
                String userResult = userTools.findUsersByNameTool(name);
                userId = extractUuid(userResult); // extract UUID from the tool result
            }
        }

        if (userId == null) {
            return "❌ Could not find the user. Please provide the user ID or search the user first.";
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

    private String extractNameFromHitlMessage(String message) {
        String m = message.toLowerCase().trim();
        // "make user X as admin", "make user X admin"
        if (m.contains("make user ")) {
            String after = message.substring(m.indexOf("make user ") + "make user ".length()).trim();
            // Take the first word as the name
            return after.split("\\s+")[0];
        }
        // "delete user X", "promote user X"
        String[] triggers = { "delete user ", "promote user ", "demote user ", "assign admin to ",
                "assign admin role to ", // ← add this
                "assign role to ", // ← add this
                "give admin to ", // ← add this
                "give admin role to " };
        for (String trigger : triggers) {
            if (m.contains(trigger)) {
                String after = message.substring(m.indexOf(trigger) + trigger.length()).trim();
                return after.split("\\s+")[0];
            }
        }
        return null;
    }

    private String extractUuid(String text) {
        if (text == null)
            return null;
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