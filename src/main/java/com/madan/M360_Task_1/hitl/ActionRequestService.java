package com.madan.M360_Task_1.hitl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madan.M360_Task_1.models.ActionRequest;
import com.madan.M360_Task_1.models.Role;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.repository.ActionRequestRepository;
import com.madan.M360_Task_1.repository.RoleRepository;
import com.madan.M360_Task_1.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ActionRequestService {

    private final ActionRequestRepository actionRequestRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;

    public ActionRequestService(ActionRequestRepository actionRequestRepository,
                                UserRepository userRepository,
                                RoleRepository roleRepository,
                                ObjectMapper objectMapper) {
        this.actionRequestRepository = actionRequestRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.objectMapper = objectMapper;
    }

    private ActionRequest createRequest(ActionType type, String payload, String requestedBy) {
        ActionRequest req = ActionRequest.builder()
                .actionType(type)
                .status(ActionStatus.PENDING)
                .payload(payload)
                .requestedBy(requestedBy)
                .requestedAt(LocalDateTime.now())
                .build();

        return actionRequestRepository.save(req);
    }

    // CREATE PENDING REQUESTS
    public ActionRequest requestDeleteUser(String userId, String requestedBy) {
        String payload = toPayloadJson(new ActionPayload(userId, null));
        return createRequest(ActionType.DELETE_USER, payload, requestedBy);
    }

    public ActionRequest requestAssignRole(String userId, String roleName, String requestedBy) {
        String payload = toPayloadJson(new ActionPayload(userId, roleName));
        return createRequest(ActionType.ASSIGN_ROLE, payload, requestedBy);
    }

    public ActionRequest requestRemoveRole(String userId, String roleName, String requestedBy) {
        String payload = toPayloadJson(new ActionPayload(userId, roleName));
        return createRequest(ActionType.REMOVE_ROLE, payload, requestedBy);
    }

    // LIST REQUESTS
    public List<ActionRequest> getByStatus(ActionStatus status) {
        return actionRequestRepository.findByStatus(status);
    }

    public ActionRequest getById(UUID id) {
        return actionRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ActionRequest not found: " + id));
    }


    // APPROVE / REJECT
    @Transactional
    public ActionRequest approveAndExecute(UUID requestId, String reviewedBy, String reason) {
        ActionRequest req = getById(requestId);

        if (req.getStatus() != ActionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PENDING requests can be approved. Current status: " + req.getStatus());
        }

        req.setStatus(ActionStatus.APPROVED);
        req.setReviewedBy(reviewedBy);
        req.setReviewedAt(LocalDateTime.now());
        req.setDecisionReason(reason);

        // Execute deterministically
        try {
            String result = execute(req);
            req.setStatus(ActionStatus.EXECUTED);
            req.setExecutedAt(LocalDateTime.now());
            req.setExecutionResult(result);
        } catch (Exception e) {
            req.setStatus(ActionStatus.FAILED);
            req.setExecutedAt(LocalDateTime.now());
            req.setExecutionError(e.getMessage());
        }

        return actionRequestRepository.save(req);
    }

    @Transactional
    public ActionRequest reject(UUID requestId, String reviewedBy, String reason) {
        ActionRequest req = getById(requestId);

        if (req.getStatus() != ActionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PENDING requests can be rejected. Current status: " + req.getStatus());
        }

        req.setStatus(ActionStatus.REJECTED);
        req.setReviewedBy(reviewedBy);
        req.setReviewedAt(LocalDateTime.now());
        req.setDecisionReason(reason);

        return actionRequestRepository.save(req);
    }

    // EXECUTION (DETERMINISTIC)
    private String execute(ActionRequest req) throws Exception {
        ActionPayload payload = fromPayloadJson(req.getPayload());

        UUID userUuid = UUID.fromString(payload.userId());

        return switch (req.getActionType()) {
            case DELETE_USER -> executeDeleteUser(userUuid);
            case ASSIGN_ROLE -> executeAssignRole(userUuid, payload.roleName());
            case REMOVE_ROLE -> executeRemoveRole(userUuid, payload.roleName());
        };
    }

    private String executeDeleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
        userRepository.deleteById(userId);
        return "Deleted user: " + userId;
    }

    private String executeAssignRole(UUID userId, String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleName is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        Role role = roleRepository.findByRoleNameIgnoreCase(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + roleName));

        user.getRoles().add(role);
        userRepository.save(user);

        return "Assigned role " + roleName.toUpperCase() + " to user " + userId;
    }

    private String executeRemoveRole(UUID userId, String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleName is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        Role role = roleRepository.findByRoleNameIgnoreCase(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + roleName));

        user.getRoles().remove(role);
        userRepository.save(user);

        return "Removed role " + roleName.toUpperCase() + " from user " + userId;
    }


    // JSON HELPERS
    private String toPayloadJson(ActionPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize payload");
        }
    }

    private ActionPayload fromPayloadJson(String json) {
        try {
            return objectMapper.readValue(json, ActionPayload.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload JSON: " + json);
        }
    }
}