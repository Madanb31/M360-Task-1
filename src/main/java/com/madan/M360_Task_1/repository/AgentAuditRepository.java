package com.madan.M360_Task_1.repository;

import com.madan.M360_Task_1.models.AgentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AgentAuditRepository extends JpaRepository<AgentAuditLog, UUID> {
    // We can add findByChatId, etc. later
}
