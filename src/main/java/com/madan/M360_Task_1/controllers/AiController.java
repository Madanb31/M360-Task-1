package com.madan.M360_Task_1.controllers;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import com.madan.M360_Task_1.ai.AdminOrchestratorAgent;
import com.madan.M360_Task_1.ai.ReadOnlyOrchestratorAgent;
import com.madan.M360_Task_1.ai.OrchestratorStreamService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final ChatMemory chatMemory;
    private final ReadOnlyOrchestratorAgent readOnlyOrchestratorAgent;
    private final AdminOrchestratorAgent adminOrchestratorAgent;
    private final OrchestratorStreamService orchestratorStreamService;

    public AiController(ReadOnlyOrchestratorAgent readOnlyOrchestratorAgent,
                        AdminOrchestratorAgent adminOrchestratorAgent,
                        ChatMemory chatMemory,
                        OrchestratorStreamService orchestratorStreamService) {
        this.readOnlyOrchestratorAgent = readOnlyOrchestratorAgent;
        this.adminOrchestratorAgent = adminOrchestratorAgent;
        this.chatMemory = chatMemory;
        this.orchestratorStreamService = orchestratorStreamService;
    }

    // USER + ADMIN (read-only)
    @PostMapping("/orchestrate")
    public String orchestrate(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String chatId = request.getOrDefault("chatId", "chat-default");
        return readOnlyOrchestratorAgent.orchestrate(message, chatId);
    }

    @GetMapping("/orchestrate/stream")
    public SseEmitter orchestrateStream(@RequestParam String message,
                                        @RequestParam(defaultValue = "chat-default") String chatId) {
        return orchestratorStreamService.stream(message, chatId, false);
    }

    // ADMIN only (read + write)
    @PostMapping("/admin/orchestrate")
    public String adminOrchestrate(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String chatId = request.getOrDefault("chatId", "admin-chat-default");
        return adminOrchestratorAgent.orchestrate(message, chatId);
    }

    @GetMapping("/admin/orchestrate/stream")
    public SseEmitter adminOrchestrateStream(@RequestParam String message,
                                             @RequestParam(defaultValue = "admin-chat-default") String chatId) {
        return orchestratorStreamService.stream(message, chatId, true);
    }

    @GetMapping("/history")
    public List<Message> getHistory(@RequestParam String chatId) {
        return chatMemory.get(chatId);
    }
}