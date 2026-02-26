package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.ai.AdminOrchestratorAgent;
import com.madan.M360_Task_1.ai.ReadOnlyOrchestratorAgent;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final ReadOnlyOrchestratorAgent readOnlyOrchestratorAgent;
    private final AdminOrchestratorAgent adminOrchestratorAgent;

    public AiController(ReadOnlyOrchestratorAgent readOnlyOrchestratorAgent,
                        AdminOrchestratorAgent adminOrchestratorAgent) {
        this.readOnlyOrchestratorAgent = readOnlyOrchestratorAgent;
        this.adminOrchestratorAgent = adminOrchestratorAgent;
    }

    // USER + ADMIN (read-only)
    @PostMapping("/orchestrate")
    public String orchestrate(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String chatId = request.getOrDefault("chatId", "chat-default");
        return readOnlyOrchestratorAgent.orchestrate(message, chatId);
    }

    // ADMIN only (read + write)
    @PostMapping("/admin/orchestrate")
    public String adminOrchestrate(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String chatId = request.getOrDefault("chatId", "admin-chat-default");
        return adminOrchestratorAgent.orchestrate(message, chatId);
    }
}