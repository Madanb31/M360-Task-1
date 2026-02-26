package com.madan.M360_Task_1.ai;

import com.madan.M360_Task_1.ai.tools.UserManagementTools;
import com.madan.M360_Task_1.models.AgentAuditLog;
import com.madan.M360_Task_1.repository.AgentAuditRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserManagementAgent {

    private final ChatClient chatClient;
    private final AgentAuditRepository auditRepository;

    public UserManagementAgent(ChatClient.Builder builder,
                               UserManagementTools managementTools,
                               AgentAuditRepository auditRepository) {

        this.auditRepository = auditRepository;

        this.chatClient = builder
                .defaultSystem("""
                You are a User Management Agent (Admin Level).
                
                Your job:
                - Create new users
                - Delete users
                - Assign or remove roles (ADMIN, USER)
                
                STRICT Rules:
                - ALWAYS use tools to perform actions.
                - Confirm the action was successful based on the tool's output.
                - If a tool returns an error, report it clearly to the user.
                - Do not make up IDs or data. Use what is provided.
                """)
                .defaultTools(managementTools) // Register the new tools
                .build();
    }

    public String manage(String userMessage, String chatId) {
        long startTime = System.currentTimeMillis();

        ChatResponse response = chatClient.prompt()
                .user(userMessage)
                .call()
                .chatResponse();

        long endTime = System.currentTimeMillis();
        String content = response.getResult().getOutput().getText();

        // Log the action
        int tokens = (response.getMetadata() != null && response.getMetadata().getUsage() != null)
                ? response.getMetadata().getUsage().getTotalTokens() : 0;

        saveLog("UserManagementAgent", userMessage, content, chatId, endTime - startTime, tokens);

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