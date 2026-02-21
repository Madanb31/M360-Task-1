package com.madan.M360_Task_1.ai;

import com.madan.M360_Task_1.ai.tools.UserTools;
import com.madan.M360_Task_1.dto.ai.AllUsersReport;
import com.madan.M360_Task_1.dto.ai.UserAnalysis;
import com.madan.M360_Task_1.models.AgentAuditLog;
import com.madan.M360_Task_1.repository.AgentAuditRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

import java.time.LocalDateTime;

@Service
public class UserAnalysisAgent {

    private final ChatClient chatClient;
    private final ChatClient jsonClient;

    private final AgentAuditRepository auditRepository;

    public UserAnalysisAgent(ChatClient.Builder builder, UserTools userTools,ChatMemory chatMemory,AgentAuditRepository auditRepository) {

        this.auditRepository = auditRepository;

        this.chatClient = builder
                .defaultSystem("""
                You are a User Analysis Agent for a User Management System.
                
                Your job:
                - Analyze user profiles
                - Search for users
                - Identify incomplete profiles
                - Provide recommendations
                
                STRICT Rules:
                - ALWAYS call tools first, THEN respond
                - NEVER explain what you're going to do
                - NEVER ask for permission
                - NEVER say "I will" or "I need to" or "Let me"
                - Just DO IT and show results
                - Use ALL available tools as needed
                - Call multiple tools in sequence automatically
                - If you need to check each user, DO IT without asking
                
                Response Format:
                - Use ✅ for complete fields
                - Use ❌ for missing fields
                - Use 📋 for profile headers
                - End with summary and recommendation
                - Be concise, show only results
                """)
                .defaultTools(userTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        // 2. JSON Client (No tools, just formatting)
        this.jsonClient = builder
                .defaultSystem("You are a data formatter. Convert text to JSON.")
                .build();
    }

    // 1. Text Analysis with Audit Trail
    public String analyze(String userMessage, String chatId) {
        long startTime = System.currentTimeMillis();

        // Get Full Response Object (not just string)
        ChatResponse response = chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", chatId))
                .call()
                .chatResponse(); // ← CHANGE: get full response object

        long endTime = System.currentTimeMillis();

        // Extract content
        String content = response.getResult().getOutput().getText();

        // Extract token usage (safe check for nulls)
        int tokens = 0;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            tokens = response.getMetadata().getUsage().getTotalTokens();
        }

        // Save Audit Log
        saveLog("UserAnalysisAgent", userMessage, content, chatId, endTime - startTime, tokens);

        return content;
    }

    // Helper to save log
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

    // 2. Structured: Single User
    public UserAnalysis analyzeUser(String name) {
        // Step A
        String rawData = chatClient.prompt()
                .user("Use userSearchTool to find '" + name + "'. Then use userLookupTool to get details. Return raw output.")
                .call()
                .content();

        System.out.println("DEBUG RAW DATA: " + rawData);  // ← Add this

        // Step B
        return jsonClient.prompt()
                .user("Here is user data: \n" + rawData + "\nMap to UserAnalysis JSON.")
                .call()
                .entity(UserAnalysis.class);
    }

    // 3. Structured: All Users
    public AllUsersReport analyzeAllUsers() {
        // Step A: Get raw data using TOOLS
        String rawData = chatClient.prompt()
                .user("Call listAllUsersTool to get all users. Return raw output.")
                .call()
                .content();

        // Step B: Convert to JSON using NO TOOLS
        return jsonClient.prompt()
                .user("Here is the list: \n" + rawData + "\nMap to AllUsersReport JSON.")
                .call()
                .entity(AllUsersReport.class);
    }

}