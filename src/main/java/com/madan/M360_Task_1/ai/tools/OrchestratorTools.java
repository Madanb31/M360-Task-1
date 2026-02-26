package com.madan.M360_Task_1.ai.tools;

import com.madan.M360_Task_1.ai.UserAnalysisAgent;
import com.madan.M360_Task_1.ai.UserManagementAgent;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrchestratorTools {

    @Autowired
    private UserAnalysisAgent analysisAgent;

    @Autowired
    private UserManagementAgent managementAgent;

    @Tool(description = "Analyzes user data, searches for users, finds IDs, and checks profile completeness. Returns analysis report.")
    public String analyzeTool(@ToolParam(description = "The question or command for the analysis agent") String message) {
        // We pass "orchestrator" as chatId so we can track internal calls in logs
        return analysisAgent.analyze(message, "internal-orchestrator");
    }

    @Tool(description = "Manages users: creates users, deletes users, assigns or removes roles. Returns result of the action.")
    public String manageTool(@ToolParam(description = "The command for the management agent") String message) {
        return managementAgent.manage(message, "internal-orchestrator");
    }
}
