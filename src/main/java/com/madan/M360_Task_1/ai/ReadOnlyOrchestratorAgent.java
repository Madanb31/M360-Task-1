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

    public ReadOnlyOrchestratorAgent(ChatClient.Builder builder,
                                     ReadOnlyOrchestratorTools readOnlyTools,
                                     UserTools userTools,
                                     RagTools ragTools,
                                     ChatMemory chatMemory,
                                     AgentAuditRepository auditRepository) {

        this.chatMemory = chatMemory;
        this.auditRepository = auditRepository;
        this.userTools = userTools;

        this.chatClient = builder
                .defaultSystem("""
                    You are the Read-Only Orchestrator Agent.
                    You are strictly READ-ONLY. You MUST NOT modify data.

                    You are an AI assistant with the ability to control the user interface directly. You have access to the following UI control tools — use them immediately when the user asks:
                    - toggleUiMode(mode) — call this when the user asks to switch/toggle/change the theme, dark mode, light mode, or UI appearance. Pass "dark" or "light" as the mode.
                    - goToPage(pageId) — call this when the user asks to navigate, go to, or open a page. Valid pageIds are: users, roles, approvals, knowledge, chat, dashboard.
                    - switchBackgroundToColour(colourCode) — change the background colour of the UI.
                    - openPageWithColour(pageName, colourCode) — navigate to a page AND change background colour at the same time.

                    When a tool returns PENDING: ... — tell the user the action is ready and they need to confirm it in the chat UI
                    When a tool returns ERROR: ... — apologize and explain exactly what went wrong using the error message
                    Never say you cannot perform UI actions — you have the tools to do it
                    """)
                .defaultTools(readOnlyTools, userTools,ragTools)
                .build();
    }

    public String orchestrate(String userMessage, String chatId) {
        return orchestrateWithThinking(userMessage, chatId, null).answer();
    }

    public ThinkingResponse orchestrateWithThinking(String userMessage, String chatId, List<AgUiParameters.FrontendToolDefinition> frontendTools) {
        long startTime = System.currentTimeMillis();

        List<Message> history = chatMemory.get(chatId);


        String deterministic = handleDeterministicRead(userMessage);
        if (deterministic != null) {
            // save memory
            chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
            chatMemory.add(chatId, List.of(new AssistantMessage(deterministic)));

            // audit
            saveLog("ReadOnlyOrchestratorAgent", userMessage, deterministic, chatId,
                    System.currentTimeMillis() - startTime, 0);

            return new ThinkingResponse(deterministic, null);
        }

        String fullUserMessage = userMessage;
        try {
            if (frontendTools != null && !frontendTools.isEmpty()) {
                String frontendToolsPrompt = """
                    You also have access to these UI control tools that execute in the frontend:
                    %s
                    When you decide to call one of these tools, output ONLY a JSON block formatted exactly like this:
                    {"toolCall": "toolName", "arguments": {"arg1": "val1"}}
                    Do not output any other text.
                    """.formatted(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(frontendTools));
                fullUserMessage = userMessage + "\n\n" + frontendToolsPrompt;
            }
        } catch (Exception e) {}

        ChatResponse response = chatClient.prompt()
                .messages(history)
                .user(fullUserMessage)
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

        saveLog("ReadOnlyOrchestratorAgent", userMessage, content, chatId,
                System.currentTimeMillis() - startTime, tokens);

        return new ThinkingResponse(content, thinking);
    }

    public ThinkingResponse resumeWithHistory(List<Message> history, String chatId, List<AgUiParameters.FrontendToolDefinition> frontendTools) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (frontendTools != null && !frontendTools.isEmpty() && !history.isEmpty()) {
                Message lastMessage = history.get(history.size() - 1);
                if (lastMessage instanceof UserMessage) {
                    String frontendToolsPrompt = """
                        You also have access to these UI control tools that execute in the frontend:
                        %s
                        When you decide to call one of these tools, output ONLY a JSON block formatted exactly like this:
                        {"toolCall": "toolName", "arguments": {"arg1": "val1"}}
                        Do not output any other text.
                        """.formatted(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(frontendTools));
                    
                    String updatedContent = lastMessage.getText() + "\n\n" + frontendToolsPrompt;
                    history.set(history.size() - 1, new UserMessage(updatedContent));
                }
            }
        } catch (Exception e) {}

        ChatResponse response;
        try {
            response = chatClient.prompt()
                    .messages(history)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null) {
                return new ThinkingResponse("I've noted your response.", "");
            }
        } catch (java.util.NoSuchElementException e) {
            return new ThinkingResponse("Action noted. Let me know if you need anything else.", "");
        } catch (Exception e) {
            return new ThinkingResponse("Action noted. Let me know if you need anything else.", "");
        }
        
        AssistantMessage assistantMessage = response.getResult().getOutput();
        String content = assistantMessage.getText();
        String thinking = "Resuming based on frontend tool result...";

        chatMemory.add(chatId, List.of(new AssistantMessage(content)));
        
        int tokens = response.getMetadata() != null && response.getMetadata().getUsage() != null ? 
            response.getMetadata().getUsage().getTotalTokens() : 0;
            
        saveLog("ReadOnlyOrchestratorAgent", "Tool Result Resumption", content, chatId,
                System.currentTimeMillis() - startTime, tokens);

        return new ThinkingResponse(content, thinking);
    }

    private String handleDeterministicRead(String msg) {
        if (msg == null) return null;
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
            if (!name.isBlank()) return userTools.findUsersByNameTool(name);
        }
        if (m.startsWith("search user ")) {
            String name = msg.substring("search user ".length()).trim();
            if (!name.isBlank()) return userTools.findUsersByNameTool(name);
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