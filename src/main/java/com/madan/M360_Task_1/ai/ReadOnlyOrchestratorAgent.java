package com.madan.M360_Task_1.ai;

import com.madan.M360_Task_1.ai.tools.RagTools;
import com.madan.M360_Task_1.ai.tools.ReadOnlyOrchestratorTools;
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
public class ReadOnlyOrchestratorAgent {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final AgentAuditRepository auditRepository;
    private final UserTools userTools;
    private final RagTools ragTools;

    public ReadOnlyOrchestratorAgent(ChatClient.Builder builder,
            ReadOnlyOrchestratorTools readOnlyTools,
            UserTools userTools,
            RagTools ragTools,
            ChatMemory chatMemory,
            AgentAuditRepository auditRepository) {

        this.chatMemory = chatMemory;
        this.auditRepository = auditRepository;
        this.userTools = userTools;
        this.ragTools = ragTools;

        this.chatClient = builder
                .defaultSystem("""
                        You are the Read-Only Orchestrator Agent.
                        You are strictly READ-ONLY. You MUST NOT modify data.

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
                .defaultTools(readOnlyTools, userTools, ragTools)
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
                // save memory
                chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
                chatMemory.add(chatId, List.of(new AssistantMessage(deterministic)));
            }

            // audit
            saveLog("ReadOnlyOrchestratorAgent", userMessage, deterministic, chatId,
                    System.currentTimeMillis() - startTime, 0);

            return new ThinkingResponse(deterministic, null);
        }

        String fullUserMessage = userMessage;
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
                fullUserMessage = userMessage + "\n\n" + frontendToolsPrompt;
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

        if (saveToMemory) {
            chatMemory.add(chatId, List.of(new UserMessage(userMessage)));

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

        saveLog("ReadOnlyOrchestratorAgent", userMessage, content, chatId,
                System.currentTimeMillis() - startTime, tokens);

        return new ThinkingResponse(content, thinking);
    }

    private String handleDeterministicRead(String msg) {
        if (msg == null)
            return null;
        String m = msg.trim().toLowerCase();

        // list all users
        if (m.equals("list all users") || m.equals("list users") || m.equals("show all users")
                || m.equals("display all users")) {
            return userTools.listAllUsersDetailedTool();
        }

        // list all admins
        if (m.equals("list all admins") || m.equals("list admins") || m.equals("show admins")
                || m.equals("display admins")) {
            return userTools.listUsersByRoleTool("ADMIN");
        }

        // analyze all users completeness report
        if (m.contains("profile completeness") && m.contains("all")
                || m.contains("analysis report") && m.contains("all")
                || m.contains("analyze all users")
                || m.contains("analyse all users")) {
            return userTools.analyzeAllUsersProfilesTool();
        }

        // find/search user <name>
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