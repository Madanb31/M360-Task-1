package com.madan.M360_Task_1.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Aspect
@Component
public class ToolExecutionStreamingAspect {

    private final ObjectMapper objectMapper;

    public ToolExecutionStreamingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object streamToolExecution(ProceedingJoinPoint pjp) throws Throwable {
        SseEmitter emitter = OrchestratorStreamService.CURRENT_EMITTER.get();
        String toolName = pjp.getSignature().getName();

        if (emitter != null) {
            try {
                String message = "Searching knowledge base...";
                String jsonEvent = objectMapper.writeValueAsString(Map.of(
                        "type", "tool_start",
                        "tool", toolName,
                        "message", message
                ));
                emitter.send(SseEmitter.event().name("step").data(jsonEvent));
            } catch (Exception e) {
                // Ignore SSE errors so tool execution continues
            }
        }

        Object result = pjp.proceed();

        if (emitter != null) {
            try {
                String message = "Search completed";
                String jsonEvent = objectMapper.writeValueAsString(Map.of(
                        "type", "tool_done",
                        "tool", toolName,
                        "message", message
                ));
                emitter.send(SseEmitter.event().name("step").data(jsonEvent));
            } catch (Exception e) {
                // Ignore SSE errors
            }
        }

        return result;
    }
}
