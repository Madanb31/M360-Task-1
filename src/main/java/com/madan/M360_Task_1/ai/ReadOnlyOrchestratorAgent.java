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
    private final ChatClient simpleChatClient;
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

        this.simpleChatClient = builder.build();

        this.chatClient = builder
                .defaultSystem("""
                    You are the Read-Only Orchestrator Agent.
                    You are strictly READ-ONLY. You MUST NOT modify data.
                    """)
                .defaultTools(readOnlyTools, userTools,ragTools)
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
            // save memory
            chatMemory.add(chatId, List.of(new UserMessage(userMessage)));
            chatMemory.add(chatId, List.of(new AssistantMessage(deterministic)));

            // audit
            saveLog("ReadOnlyOrchestratorAgent", userMessage, deterministic, chatId,
                    System.currentTimeMillis() - startTime, 0);

            return new ThinkingResponse(deterministic, null);
        }

        ChatResponse response = chatClient.prompt()
                .messages(history)
                .user(userMessage)
                .call()
                .chatResponse();

        AssistantMessage assistantMessage = response.getResult().getOutput();
        String content = assistantMessage.getText();

        String thinking = null;
        try {
            thinking = simpleChatClient.prompt()
                    .user("In 2-3 sentences, what information was found and used to answer this question? Question: " + userMessage + " Answer: " + content)
                    .call()
                    .content();
        } catch (Exception e) {
            // Fallback if quota exceeded or any error
            thinking = "Searching company knowledge base and documents to find relevant information for: \"" + userMessage + "\"";
        }

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