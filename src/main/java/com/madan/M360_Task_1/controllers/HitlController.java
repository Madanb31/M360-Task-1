package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.hitl.ActionRequestService;
import com.madan.M360_Task_1.hitl.ActionStatus;
import com.madan.M360_Task_1.models.ActionRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/hitl/actions")
public class HitlController {

    private final ActionRequestService actionRequestService;

    public HitlController(ActionRequestService actionRequestService) {
        this.actionRequestService = actionRequestService;
    }


    @GetMapping
    public List<ActionRequest> listByStatus(@RequestParam(defaultValue = "PENDING") ActionStatus status) {
        return actionRequestService.getByStatus(status);
    }


    @PostMapping("/{id}/approve")
    public ActionRequest approve(@PathVariable UUID id,
                                 @RequestBody(required = false) Map<String, String> body,
                                 Authentication authentication) {

        String reviewedBy = authentication.getName(); // username from JWT
        String reason = body != null ? body.getOrDefault("reason", "") : "";

        return actionRequestService.approveAndExecute(id, reviewedBy, reason);
    }

    @PostMapping("/{id}/reject")
    public ActionRequest reject(@PathVariable UUID id,
                                @RequestBody(required = false) Map<String, String> body,
                                Authentication authentication) {

        String reviewedBy = authentication.getName();
        String reason = body != null ? body.getOrDefault("reason", "") : "";

        return actionRequestService.reject(id, reviewedBy, reason);
    }
}