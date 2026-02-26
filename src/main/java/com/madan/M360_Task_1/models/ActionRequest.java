package com.madan.M360_Task_1.models;

import com.madan.M360_Task_1.hitl.ActionStatus;
import com.madan.M360_Task_1.hitl.ActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "action_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;

    // Store action target details as JSON-like string for now
    @Column(nullable = false, length = 4000)
    private String payload;

    // Who requested this action (username)
    @Column(nullable = false)
    private String requestedBy;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    // Who reviewed this action (username)
    private String reviewedBy;
    private LocalDateTime reviewedAt;

    // Optional reason/notes for approval/rejection
    @Column(length = 2000)
    private String decisionReason;

    // Execution result fields
    private LocalDateTime executedAt;

    @Column(length = 4000)
    private String executionResult;

    @Column(length = 4000)
    private String executionError;
}