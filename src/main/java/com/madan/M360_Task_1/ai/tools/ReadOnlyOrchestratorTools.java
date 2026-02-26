package com.madan.M360_Task_1.ai.tools;

import com.madan.M360_Task_1.ai.UserAnalysisAgent;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ReadOnlyOrchestratorTools {

    private final UserAnalysisAgent analysisAgent;

    public ReadOnlyOrchestratorTools(UserAnalysisAgent analysisAgent) {
        this.analysisAgent = analysisAgent;
    }

    @Tool(description = "Read-only analysis tool. Use this to analyze/search users and generate reports. Never modifies data.")
    public String analyzeTool(@ToolParam(description = "User request for analysis") String message) {
        return analysisAgent.analyze(message, "internal-orchestrator-readonly");
    }
}