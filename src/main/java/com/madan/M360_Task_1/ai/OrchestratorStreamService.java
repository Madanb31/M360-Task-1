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

    public OrchestratorStreamService(ReadOnlyOrchestratorAgent readOnlyOrchestratorAgent, AdminOrchestratorAgent adminOrchestratorAgent, ChatMemory chatMemory) {
        this.readOnlyOrchestratorAgent = readOnlyOrchestratorAgent;
        this.adminOrchestratorAgent = adminOrchestratorAgent;
        this.chatMemory = chatMemory;
    }

    public SseEmitter stream(AgUiParameters parameters, boolean isAdmin) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        emitter.onTimeout(() -> emitter.complete());
        emitter.onError((e) -> emitter.complete());
        emitter.onCompletion(() -> System.out.println("SSE completed"));
        
        String safeChatId = parameters.getChatId() != null ? parameters.getChatId() : (isAdmin ? "admin-chat-default" : "chat-default");
        
        if (parameters.getFrontendTools() != null) {
            chatFrontendTools.put(safeChatId, parameters.getFrontendTools());
        }

        executor.execute(() -> {
            CURRENT_EMITTER.set(emitter);
            try {
                ThinkingResponse response;
                if (isAdmin) {
                    response = adminOrchestratorAgent.orchestrateWithThinking(parameters.getMessage(), safeChatId, parameters.getFrontendTools(), true);
                } else {
                    response = readOnlyOrchestratorAgent.orchestrateWithThinking(parameters.getMessage(), safeChatId, parameters.getFrontendTools(), true);
                }

                String answer = response.answer();
                
                // Fallback check: look for JSON formatted ToolCall in the response text
                try {
                    String cleanAnswer = answer.trim();
                    if (cleanAnswer.startsWith("```json")) {
                        cleanAnswer = cleanAnswer.substring(7);
                    } else if (cleanAnswer.startsWith("```")) {
                        cleanAnswer = cleanAnswer.substring(3);
                    }
                    if (cleanAnswer.endsWith("```")) {
                        cleanAnswer = cleanAnswer.substring(0, cleanAnswer.length() - 3);
                    }
                    cleanAnswer = cleanAnswer.trim();

                    // Extract just the JSON object in case Gemini adds preamble text
                    int jsonStart = cleanAnswer.indexOf('{');
                    int jsonEnd = cleanAnswer.lastIndexOf('}');
                    String remainingText = "";
                    if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                        remainingText = cleanAnswer.substring(jsonEnd + 1).trim();
                        cleanAnswer = cleanAnswer.substring(jsonStart, jsonEnd + 1);
                    } else {
                        // Throw exception to skip parsing and fall into normal answer flow
                        throw new Exception("No valid JSON object found in answer");
                    }

                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(cleanAnswer);
                    if (node.has("toolCall")) {
                        String toolName = node.get("toolCall").asText();
                        com.fasterxml.jackson.databind.JsonNode argsNode = node.get("arguments");

                        boolean isFrontendTool = parameters.getFrontendTools() != null &&
                            parameters.getFrontendTools().stream()
                                .anyMatch(t -> t.getName().equals(toolName));

                        if (isFrontendTool) {
                            Map<String, Object> args = objectMapper.convertValue(argsNode, Map.class);
                            String toolCallId = UUID.randomUUID().toString();

                            String jsonEvent = objectMapper.writeValueAsString(Map.of(
                                "type", "TOOL_CALL",
                                "toolCallId", toolCallId,
                                "tool", toolName,
                                "args", args
                            ));
                            emitter.send(SseEmitter.event().name("tool_call").data(jsonEvent));
                            
                            if (!remainingText.isEmpty()) {
                                // Stream the remaining text as the answer
                                emitter.send(SseEmitter.event().name("answer")
                                    .data(objectMapper.writeValueAsString(Map.of("answer", remainingText))));
                            }
                            
                            emitter.complete();
                            return;
                        }
                    }
                } catch (Exception e) {
                    // Not valid JSON or not a tool call, proceed normal response
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
                String jsonAnswer = objectMapper.writeValueAsString(Map.of("answer", answer));
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
                    ? "The user accepted the " + body.getToolName() + " action. It has been executed successfully in the UI. Do NOT repeat or re-answer any previous questions — just briefly confirm the UI action was completed."
                    : "The user rejected the " + body.getToolName() + " action. Do NOT repeat or re-answer any previous questions — just briefly acknowledge the rejection.";

                // Retrieve cached frontend tools for this chatId
                List<FrontendToolDefinition> frontendTools =
                    chatFrontendTools.getOrDefault(body.getChatId(), new ArrayList<>());

                // ChatMemory already has the full history — just call orchestrateWithThinking directly
                ThinkingResponse response = isAdmin
                    ? adminOrchestratorAgent.orchestrateWithThinking(toolResultMessage, body.getChatId(), frontendTools, false)
                    : readOnlyOrchestratorAgent.orchestrateWithThinking(toolResultMessage, body.getChatId(), frontendTools, false);

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
