package com.madan.M360_Task_1.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OrchestratorStreamService {

    public static final ThreadLocal<SseEmitter> CURRENT_EMITTER = new ThreadLocal<>();

    private final ReadOnlyOrchestratorAgent readOnlyOrchestratorAgent;
    private final AdminOrchestratorAgent adminOrchestratorAgent;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrchestratorStreamService(ReadOnlyOrchestratorAgent readOnlyOrchestratorAgent, AdminOrchestratorAgent adminOrchestratorAgent) {
        this.readOnlyOrchestratorAgent = readOnlyOrchestratorAgent;
        this.adminOrchestratorAgent = adminOrchestratorAgent;
    }

    public SseEmitter stream(String userMessage, String chatId, boolean isAdmin) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        emitter.onTimeout(() -> emitter.complete());
        emitter.onError((e) -> emitter.complete());
        emitter.onCompletion(() -> System.out.println("SSE completed"));

        executor.execute(() -> {
            CURRENT_EMITTER.set(emitter);
            try {
                ThinkingResponse response;
                if (isAdmin) {
                    response = adminOrchestratorAgent.orchestrateWithThinking(userMessage, chatId);
                } else {
                    response = readOnlyOrchestratorAgent.orchestrateWithThinking(userMessage, chatId);
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
