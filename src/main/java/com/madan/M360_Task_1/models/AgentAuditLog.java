package com.madan.M360_Task_1.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentAuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    private String agentName;       // e.g., "UserAnalysisAgent"

    @Column(length = 5000)
    private String userQuery;       // "Tell me about Madan"

    @Column(length = 5000)
    private String aiResponse;      // "Madan is a user..."

    private String chatId;          // "session-1"

    private LocalDateTime timestamp;

    // Performance Metrics
    private long executionTimeMs;   // How long it took
    private int totalTokens;        // Cost metric
}
