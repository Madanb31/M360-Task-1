package com.madan.M360_Task_1.repository;

import com.madan.M360_Task_1.hitl.ActionStatus;
import com.madan.M360_Task_1.models.ActionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActionRequestRepository extends JpaRepository<ActionRequest, UUID> {
    List<ActionRequest> findByStatus(ActionStatus status);
}