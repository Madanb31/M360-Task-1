package com.madan.M360_Task_1.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import com.madan.M360_Task_1.ai.AgUiParameters.FrontendToolDefinition;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.memory.ChatMemory;

@Service
public class OrchestratorStreamService {

    public static final ThreadLocal<SseEmitter> CURRENT_EMITTER = new ThreadLocal<>();
    private static final Map<String, List<FrontendToolDefinition>> chatFrontendTools = new ConcurrentHashMap<>();

    private final ReadOnlyOrchestratorAgent readOnlyOrchestratorAgent;
    private final AdminOrchestratorAgent adminOrchestratorAgent;
    private final ChatMemory chatMemory;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrchestratorStreamService(ReadOnlyOrchestratorAgent readOnlyOrchestratorAgent,
            AdminOrchestratorAgent adminOrchestratorAgent, ChatMemory chatMemory) {
        this.readOnlyOrchestratorAgent = readOnlyOrchestratorAgent;
        this.adminOrchestratorAgent = adminOrchestratorAgent;
        this.chatMemory = chatMemory;
    }

    public SseEmitter stream(AgUiParameters parameters, boolean isAdmin) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        emitter.onTimeout(() -> emitter.complete());
        emitter.onError((e) -> emitter.complete());
        emitter.onCompletion(() -> System.out.println("SSE completed"));

        String safeChatId = parameters.getChatId() != null ? parameters.getChatId()
                : (isAdmin ? "admin-chat-default" : "chat-default");

        if (parameters.getFrontendTools() != null) {
            chatFrontendTools.put(safeChatId, parameters.getFrontendTools());
        }

        executor.execute(() -> {
            CURRENT_EMITTER.set(emitter);
            try {
                ThinkingResponse response;
                if (isAdmin) {
                    response = adminOrchestratorAgent.orchestrateWithThinking(parameters.getMessage(), safeChatId,
                            parameters.getFrontendTools(), parameters.getUiState(), true);
                } else {
                    response = readOnlyOrchestratorAgent.orchestrateWithThinking(parameters.getMessage(), safeChatId,
                            parameters.getFrontendTools(), parameters.getUiState(), true);
                }

                String content = response.answer();
                boolean isFrontendToolCall = content != null && content.contains("toolCall");

                if (isFrontendToolCall) {
                    // Find the JSON block — could be at start OR end of response
                    int jsonStart = content.indexOf('{');
                    int jsonEnd = content.lastIndexOf('}');

                    if (jsonStart >= 0 && jsonEnd >= 0 && jsonEnd > jsonStart) {
                        String jsonBlock = content.substring(jsonStart, jsonEnd + 1);
                        String beforeJson = content.substring(0, jsonStart).trim();
                        String afterJson = content.substring(jsonEnd + 1).trim();

                        try {
                            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(jsonBlock);
                            if (node.has("toolCall")) {
                                String toolName = node.get("toolCall").asText();
                                Map<String, Object> args = objectMapper.convertValue(
                                        node.get("arguments"), Map.class);

                                // Check if it's a known frontend tool
                                List<AgUiParameters.FrontendToolDefinition> frontendTools = chatFrontendTools
                                        .getOrDefault(safeChatId, new ArrayList<>());
                                boolean isFrontendTool = frontendTools.stream()
                                        .anyMatch(t -> t.getName().equals(toolName));

                                if (isFrontendTool) {
                                    // Stream the text part first (before or after JSON)
                                    String textPart = beforeJson.isEmpty() ? afterJson : beforeJson;

                                    if (!textPart.isEmpty()) {
                                        // Stream thinking tokens
                                        if (response.thinkingContent() != null
                                                && !response.thinkingContent().isEmpty()) {
                                            StringTokenizer st = new StringTokenizer(response.thinkingContent(),
                                                    " \n\r\t", true);
                                            while (st.hasMoreTokens()) {
                                                String token = st.nextToken();
                                                emitter.send(SseEmitter.event().name("thinking")
                                                        .data(objectMapper.writeValueAsString(Map.of("token", token))));
                                                Thread.sleep(20);
                                            }
                                        }
                                        // Stream the answer text
                                        emitter.send(SseEmitter.event().name("answer")
                                                .data(objectMapper.writeValueAsString(
                                                        Map.of("answer", textPart))));
                                    }

                                    // Stream the TOOL_CALL event
                                    String toolCallId = UUID.randomUUID().toString();
                                    emitter.send(SseEmitter.event().name("tool_call")
                                            .data(objectMapper.writeValueAsString(Map.of(
                                                    "type", "TOOL_CALL",
                                                    "toolCallId", toolCallId,
                                                    "tool", toolName,
                                                    "args", args))));

                                    emitter.complete();
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            // JSON parsing failed — treat as normal response
                        }
                    }

                    // If we get here, treat as normal response
                    isFrontendToolCall = false;
                }

                String thinking = response.thinkingContent();
                if (thinking != null && !thinking.isEmpty()) {
                    StringTokenizer st = new StringTokenizer(thinking, " \n\r\t", true);
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        // Send each word/space/newline token as a thinking event
                        String jsonToken = objectMapper.writeValueAsString(Map.of("token", token));
                        emitter.send(SseEmitter.event().name("thinking").data(jsonToken));
                        Thread.sleep(20);
                    }
                }

                // Final answer event
                String jsonAnswer = objectMapper.writeValueAsString(Map.of("answer", content));
                emitter.send(SseEmitter.event().name("answer").data(jsonAnswer));
                emitter.complete();

            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                CURRENT_EMITTER.remove();
            }
        });

        return emitter;
    }

    public SseEmitter resumeWithToolResult(ToolResultRequest body) {
        SseEmitter emitter = new SseEmitter(180_000L);
        emitter.onTimeout(() -> emitter.complete());
        emitter.onError((e) -> emitter.complete());

        boolean isAdmin = body.isAdmin(); // or determine from chatId/security context

        executor.execute(() -> {
            CURRENT_EMITTER.set(emitter);
            try {
                // Build a natural tool result message
                String toolResultMessage = body.getResult().equals("accepted")
                        ? "The user accepted the " + body.getToolName()
                                + " action. It has been executed successfully in the UI. Do NOT repeat or re-answer any previous questions — just briefly confirm the UI action was completed."
                        : "The user rejected the " + body.getToolName()
                                + " action. Do NOT repeat or re-answer any previous questions — just briefly acknowledge the rejection.";

                // Retrieve cached frontend tools for this chatId
                List<FrontendToolDefinition> frontendTools = chatFrontendTools.getOrDefault(body.getChatId(),
                        new ArrayList<>());

                // ChatMemory already has the full history — just call orchestrateWithThinking
                // directly
                ThinkingResponse response = isAdmin
                        ? adminOrchestratorAgent.orchestrateWithThinking(toolResultMessage, body.getChatId(),
                                frontendTools, body.getUiState(), false)
                        : readOnlyOrchestratorAgent.orchestrateWithThinking(toolResultMessage, body.getChatId(),
                                frontendTools, body.getUiState(), false);

                // Save only the clean final answer to ChatMemory
                chatMemory.add(body.getChatId(), List.of(new AssistantMessage(response.answer())));

                // Stream thinking tokens if present
                if (response.thinkingContent() != null && !response.thinkingContent().isEmpty()) {
                    StringTokenizer st = new StringTokenizer(response.thinkingContent(), " \n\r\t", true);
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        String jsonToken = objectMapper.writeValueAsString(Map.of("token", token));
                        emitter.send(SseEmitter.event().name("thinking").data(jsonToken));
                        Thread.sleep(20);
                    }
                }

                // Stream final answer
                String jsonAnswer = objectMapper.writeValueAsString(Map.of("answer", response.answer()));
                emitter.send(SseEmitter.event().name("answer").data(jsonAnswer));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                CURRENT_EMITTER.remove();
            }
        });

        return emitter;
    }
}
