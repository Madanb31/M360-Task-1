package com.madan.M360_Task_1.ai.tools;

import com.madan.M360_Task_1.hitl.ActionRequestService;
import com.madan.M360_Task_1.models.ActionRequest;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class HitlRequestTools {

    private final ActionRequestService actionRequestService;

    public HitlRequestTools(ActionRequestService actionRequestService) {
        this.actionRequestService = actionRequestService;
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "unknown";
    }

    @Tool(description = "Creates a PENDING approval request to delete a user. Does NOT delete immediately. Requires human approval.")
    public String requestDeleteUserTool(
            @ToolParam(description = "UUID of the user to delete") String userId) {

        String requestedBy = currentUsername();
        ActionRequest req = actionRequestService.requestDeleteUser(userId, requestedBy);

        return """
            🛑 Approval Required (PENDING)

            RequestId: %s
            Action: DELETE_USER
            TargetUserId: %s
            Role: N/A
            RequestedBy: %s

            To Approve:
            POST /hitl/actions/%s/approve

            To Reject:
            POST /hitl/actions/%s/reject
            """.formatted(req.getId(), userId, requestedBy, req.getId(), req.getId());
    }

    @Tool(description = "Creates a PENDING approval request to assign a role to a user. Does NOT assign immediately. Requires human approval.")
    public String requestAssignRoleTool(
            @ToolParam(description = "UUID of the user") String userId,
            @ToolParam(description = "Role to assign (e.g., ADMIN)") String roleName) {

        String requestedBy = currentUsername();
        ActionRequest req = actionRequestService.requestAssignRole(userId, roleName, requestedBy);

        return """
            🛑 Approval Required (PENDING)

            RequestId: %s
            Action: ASSIGN_ROLE
            TargetUserId: %s
            Role: %s
            RequestedBy: %s

            To Approve:
            POST /hitl/actions/%s/approve

            To Reject:
            POST /hitl/actions/%s/reject
            """.formatted(req.getId(), userId, roleName, requestedBy, req.getId(), req.getId());
    }

    @Tool(description = "Creates a PENDING approval request to remove a role from a user. Does NOT remove immediately. Requires human approval.")
    public String requestRemoveRoleTool(
            @ToolParam(description = "UUID of the user") String userId,
            @ToolParam(description = "Role to remove (e.g., ADMIN)") String roleName) {

        String requestedBy = currentUsername();
        ActionRequest req = actionRequestService.requestRemoveRole(userId, roleName, requestedBy);

        return """
            🛑 Approval Required (PENDING)

            RequestId: %s
            Action: REMOVE_ROLE
            TargetUserId: %s
            Role: %s
            RequestedBy: %s

            To Approve:
            POST /hitl/actions/%s/approve

            To Reject:
            POST /hitl/actions/%s/reject
            """.formatted(req.getId(), userId, roleName, requestedBy, req.getId(), req.getId());
    }
}