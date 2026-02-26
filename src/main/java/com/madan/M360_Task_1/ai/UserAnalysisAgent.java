package com.madan.M360_Task_1.ai;

import com.madan.M360_Task_1.ai.tools.UserTools;
import com.madan.M360_Task_1.dto.ai.AllUsersReport;
import com.madan.M360_Task_1.dto.ai.UserAnalysis;
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
public class UserAnalysisAgent {

    private final ChatClient chatClient; // Agent Client
    private final ChatClient jsonClient; // JSON Formatter
    private final AgentAuditRepository auditRepository;
    private final ChatMemory chatMemory; // Inject Memory

    public UserAnalysisAgent(ChatClient.Builder builder,
                             UserTools userTools,
                             ChatMemory chatMemory,
                             AgentAuditRepository auditRepository) {

        this.auditRepository = auditRepository;
        this.chatMemory = chatMemory;

        // 1. Agent Client (Tools, NO Advisor here)
        this.chatClient = builder
                .defaultSystem("""
                You are a User Analysis Agent.
                
                Your job:
                - Analyze user profiles
                - Search for users
                - Identify incomplete profiles
                
                STRICT Rules:
                - ALWAYS call tools first, THEN respond.
                - If the user refers to a previous topic (e.g. "him", "her", "it"), check the chat history.
                - Be concise.
                """)
                .defaultTools(userTools)
                // REMOVED: .defaultAdvisors(...) -> We handle memory manually
                .build();

        // 2. JSON Client (Formatting only)
        this.jsonClient = builder
                .defaultSystem("You are a data formatter. Convert text to JSON.")
                .build();
    }

    // 1. Text Analysis (Manual Memory Implementation)
    public String analyze(String userMessage, String chatId) {
        long startTime = System.currentTimeMillis();

        // A. Retrieve History
        List<Message> history = chatMemory.get(chatId);
        System.out.println("DEBUG: History size for " + chatId + " = " + history.size());

        // B. Call AI with History + New Message
        ChatResponse response = chatClient.prompt()
                .messages(history) // Inject history
                .user(userMessage) // Add current message
                .call()
                .chatResponse();

        long endTime = System.currentTimeMillis();
        String content = response.getResult().getOutput().getText();

        // C. Save to Memory
        chatMemory.add(chatId, List.of(new UserMessage(userMessage)));       // Wrap in List
        chatMemory.add(chatId, List.of(new AssistantMessage(content)));  // Wrap in List

        // D. Audit Logging
        int tokens = 0;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            tokens = response.getMetadata().getUsage().getTotalTokens();
        }
        saveLog("UserAnalysisAgent", userMessage, content, chatId, endTime - startTime, tokens);

        return content;
    }

    // 2. Structured: Single User (2-Step Logic)
    public UserAnalysis analyzeUser(String name) {
        // Step A: Get raw data using TOOLS
        ChatResponse toolResponse = chatClient.prompt()
                .user("Use userSearchTool to find '" + name + "'. Then use userLookupTool to get details. Return raw output.")
                .call()
                .chatResponse();

        String rawData = toolResponse.getResult().getOutput().getText();

        // Step B: Convert to JSON using NO TOOLS
        return jsonClient.prompt()
                .user("Here is user data: \n" + rawData + "\nMap to UserAnalysis JSON.")
                .call()
                .entity(UserAnalysis.class);
    }

    // 3. Structured: All Users (2-Step Logic)
    public AllUsersReport analyzeAllUsers() {
        // Step A: Get raw data using TOOLS
        ChatResponse toolResponse = chatClient.prompt()
                .user("Call listAllUsersTool to get all users. Return raw output.")
                .call()
                .chatResponse(); // Use chatResponse(), NOT content() // Just get string for Step A here is fine too

        String rawData = toolResponse.getResult().getOutput().getText();

        // Step B: Convert to JSON using NO TOOLS
        return jsonClient.prompt()
                .user("Here is the list: \n" + rawData + "\nMap to AllUsersReport JSON.")
                .call()
                .entity(AllUsersReport.class);
    }

    // Helper: Save Log
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